package com.marketcompass.llm.controller;

import com.marketcompass.llm.dto.SetupRequest;
import com.marketcompass.llm.dto.TranscribeResponse;
import com.marketcompass.llm.groq.GroqLLMService;
import com.marketcompass.llm.groq.GroqSpeechService;
import com.marketcompass.llm.service.LLMService;
import com.marketcompass.llm.service.SpeechService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stt")
@Slf4j
public class SpeechController {
    @Autowired(required = false) private SpeechService localSpeechService;
    @Autowired(required = false) private LLMService localLLMService;
    @Autowired(required = false) private GroqSpeechService groqSpeechService;
    @Autowired(required = false) private GroqLLMService groqLLMService;

    @PostMapping("/setup")
    public ResponseEntity<String> setup(@RequestBody SetupRequest request, HttpSession session) {
        session.setAttribute("jobDescription", request.getJobDescription());
        session.setAttribute("resume", request.getResume());
        session.setAttribute("history", new ArrayList<Map<String, String>>());
        session.setAttribute("sessionActive", true);
        return ResponseEntity.ok("Session started");
    }

    @PostMapping("/transcribe")
    public ResponseEntity<TranscribeResponse> transcribe(@RequestParam("audio") MultipartFile audio) throws Exception {
        String transcript = groqSpeechService != null ? groqSpeechService.transcribe(audio) : localSpeechService.transcribe(audio);
        return ResponseEntity.ok(TranscribeResponse.builder().transcript(transcript).build());
    }

    @PostMapping("/answer-live")
    public ResponseEntity<TranscribeResponse> answerLive(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("sessionActive"))) {
            return ResponseEntity.badRequest().body(TranscribeResponse.builder().answer("The interview session has ended. Start a new session to continue.").build());
        }
        String conversation = String.valueOf(request.getOrDefault("conversation", "")).trim();
        if (conversation.isBlank()) {
            return ResponseEntity.badRequest().body(TranscribeResponse.builder().answer("I could not hear a question yet. Keep listening and try Answer Now again.").build());
        }
        String jd = (String) session.getAttribute("jobDescription");
        String resume = (String) session.getAttribute("resume");
        @SuppressWarnings("unchecked") List<Map<String, String>> history = (List<Map<String, String>>) session.getAttribute("history");
        if (history == null) history = new ArrayList<>();

        String answer;
        if (groqLLMService != null) {
            answer = groqLLMService.answerLiveConversation(conversation, jd, resume, history);
        } else {
            String prompt = "LIVE INTERVIEW ROLLING TRANSCRIPT:\n" + conversation +
                    "\n\nIdentify the latest interviewer question/follow-up and answer it as the candidate in first person. " +
                    "Use the candidate context, recent discussion, practical experience and natural conversational tone. Do not invent experience.";
            answer = localLLMService.answerQuestion(prompt, jd, resume);
        }

        history.add(Map.of("role", "user", "content", "Live interview:\n" + conversation));
        history.add(Map.of("role", "assistant", "content", answer));
        if (history.size() > 8) history = new ArrayList<>(history.subList(history.size() - 8, history.size()));
        session.setAttribute("history", history);

        return ResponseEntity.ok(TranscribeResponse.builder().transcript(conversation).answer(answer)
                .model(groqLLMService != null ? "groq/live-interview" : "ollama/live-interview").build());
    }

    @PostMapping("/ask")
    public ResponseEntity<TranscribeResponse> transcribeAndAsk(@RequestParam("audio") MultipartFile audio, HttpSession session) throws Exception {
        if (!Boolean.TRUE.equals(session.getAttribute("sessionActive"))) {
            return ResponseEntity.badRequest().body(TranscribeResponse.builder().answer("The interview session has ended.").build());
        }
        String jd = (String) session.getAttribute("jobDescription");
        String resume = (String) session.getAttribute("resume");
        @SuppressWarnings("unchecked") List<Map<String, String>> history = (List<Map<String, String>>) session.getAttribute("history");
        if (history == null) history = new ArrayList<>();
        TranscribeResponse response = groqSpeechService != null
                ? groqSpeechService.transcribeAndAsk(audio, jd, resume, history)
                : localSpeechService.transcribeAndAsk(audio, jd, resume, history);
        history.add(Map.of("role", "user", "content", response.getTranscript()));
        history.add(Map.of("role", "assistant", "content", response.getAnswer()));
        session.setAttribute("history", history);
        return ResponseEntity.ok(response);
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
