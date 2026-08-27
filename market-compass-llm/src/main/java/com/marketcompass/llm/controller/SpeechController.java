package com.marketcompass.llm.controller;

import com.marketcompass.llm.dto.SetupRequest;
import com.marketcompass.llm.dto.TranscribeResponse;
import com.marketcompass.llm.groq.GroqSpeechService;
import com.marketcompass.llm.groq.LiveInterviewService;
import com.marketcompass.llm.interview.CandidateProfileStore;
import com.marketcompass.llm.interview.InterviewAnalysis;
import com.marketcompass.llm.interview.InterviewGuidanceService;
import com.marketcompass.llm.service.LLMService;
import com.marketcompass.llm.service.SpeechService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stt")
public class SpeechController {
    @Autowired(required = false) private SpeechService localSpeechService;
    @Autowired(required = false) private LLMService localLLMService;
    @Autowired(required = false) private GroqSpeechService groqSpeechService;
    @Autowired(required = false) private LiveInterviewService liveInterviewService;
    @Autowired private CandidateProfileStore candidateProfileStore;
    @Autowired private InterviewGuidanceService interviewGuidanceService;

    @PostMapping("/setup")
    public ResponseEntity<String> setup(@RequestBody SetupRequest request, HttpSession session) {
        if (request.getJobDescription() == null || request.getJobDescription().isBlank()) {
            return ResponseEntity.badRequest().body("Job description is required");
        }

        String resume = request.getResume();
        if (resume != null && !resume.isBlank()) {
            candidateProfileStore.saveResume(resume);
        } else {
            resume = candidateProfileStore.load().map(p -> p.getResume()).orElse(null);
        }

        if (resume == null || resume.isBlank()) {
            return ResponseEntity.badRequest().body(
                    "No saved resume found. Upload a resume once using PUT /api/interview/profile/resume");
        }

        session.setAttribute("jobDescription", request.getJobDescription().trim());
        session.setAttribute("resume", resume);
        session.setAttribute("history", new ArrayList<Map<String, String>>());
        session.setAttribute("sessionActive", true);
        return ResponseEntity.ok("Session started using saved candidate profile");
    }

    @PostMapping("/transcribe")
    public ResponseEntity<TranscribeResponse> transcribe(@RequestParam("audio") MultipartFile audio) throws Exception {
        String transcript = groqSpeechService != null ? groqSpeechService.transcribe(audio) : localSpeechService.transcribe(audio);
        return ResponseEntity.ok(TranscribeResponse.builder().transcript(transcript).build());
    }

    @PostMapping("/answer-live")
    public ResponseEntity<TranscribeResponse> answerLive(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("sessionActive"))) {
            return ResponseEntity.badRequest().body(TranscribeResponse.builder()
                    .answer("The interview session has ended. Start a new session to continue.").build());
        }

        String conversation = String.valueOf(request.getOrDefault("conversation", "")).trim();
        if (conversation.isBlank()) {
            return ResponseEntity.badRequest().body(TranscribeResponse.builder()
                    .answer("I could not hear a question yet. Keep listening and try Answer Now again.").build());
        }

        String jd = (String) session.getAttribute("jobDescription");
        String resume = (String) session.getAttribute("resume");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) session.getAttribute("history");
        if (history == null) history = new ArrayList<>();

        String answer;
        if (liveInterviewService != null) {
            answer = liveInterviewService.answer(conversation, jd, resume, history);
        } else {
            String prompt = "LIVE INTERVIEW ROLLING TRANSCRIPT:\n" + conversation +
                    "\n\nIdentify the latest interviewer question/follow-up and answer it as the candidate in first person. " +
                    "Use the candidate context, recent discussion, practical experience and natural conversational tone. Do not invent experience.";
            answer = localLLMService.answerQuestion(prompt, jd, resume);
        }

        InterviewAnalysis analysis = interviewGuidanceService.analyze(conversation, history);
        appendHistory(history, conversation, answer, analysis);
        session.setAttribute("history", history);

        return response(conversation, answer, liveInterviewService != null ? "groq/live-interview" : "ollama/live-interview", analysis);
    }

    @PostMapping("/ask")
    public ResponseEntity<TranscribeResponse> transcribeAndAsk(@RequestParam("audio") MultipartFile audio, HttpSession session) throws Exception {
        if (!Boolean.TRUE.equals(session.getAttribute("sessionActive"))) {
            return ResponseEntity.badRequest().body(TranscribeResponse.builder().answer("The interview session has ended.").build());
        }

        String jd = (String) session.getAttribute("jobDescription");
        String resume = (String) session.getAttribute("resume");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) session.getAttribute("history");
        if (history == null) history = new ArrayList<>();

        String transcript;
        String answer;
        String model;
        if (groqSpeechService != null) {
            transcript = groqSpeechService.transcribe(audio);
            if (transcript.isBlank()) {
                return ResponseEntity.badRequest().body(TranscribeResponse.builder().transcript("")
                        .answer("I could not hear a complete question. Please try Answer Now again.").model("groq/stt").build());
            }
            if (liveInterviewService != null) {
                answer = liveInterviewService.answer(transcript, jd, resume, history);
                model = "groq/live-interview";
            } else {
                answer = groqSpeechService.transcribeAndAsk(audio, jd, resume, history).getAnswer();
                model = "groq/stt-llm";
            }
        } else {
            TranscribeResponse response = localSpeechService.transcribeAndAsk(audio, jd, resume, history);
            transcript = response.getTranscript();
            answer = response.getAnswer();
            model = response.getModel();
        }

        InterviewAnalysis analysis = interviewGuidanceService.analyze(transcript, history);
        appendHistory(history, transcript, answer, analysis);
        session.setAttribute("history", history);
        return response(transcript, answer, model, analysis);
    }

    private void appendHistory(List<Map<String, String>> history, String transcript, String answer, InterviewAnalysis analysis) {
        history.add(Map.of("role", "user", "content", "Live interview:\n" + transcript,
                "questionType", analysis.getQuestionType().name(), "topic", analysis.getTopic()));
        history.add(Map.of("role", "assistant", "content", answer));
        if (history.size() > 8) history.subList(0, history.size() - 8).clear();
    }

    private ResponseEntity<TranscribeResponse> response(String transcript, String answer, String model, InterviewAnalysis analysis) {
        return ResponseEntity.ok(TranscribeResponse.builder()
                .transcript(transcript)
                .answer(answer)
                .model(model)
                .questionType(analysis.getQuestionType().name())
                .topic(analysis.getTopic())
                .likelyFollowUps(analysis.getLikelyFollowUps())
                .build());
    }

    @PostMapping("/end-session")
    public ResponseEntity<String> endSession(HttpSession session) {
        session.setAttribute("sessionActive", false);
        return ResponseEntity.ok("Session ended");
    }

    @PostMapping("/reset-history")
    public ResponseEntity<String> resetHistory(HttpSession session) {
        session.setAttribute("history", new ArrayList<Map<String, String>>());
        session.setAttribute("sessionActive", false);
        return ResponseEntity.ok("Session reset");
    }
}
