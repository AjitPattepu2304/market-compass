package com.marketcompass.llm.controller;

import com.marketcompass.llm.dto.SetupRequest;
import com.marketcompass.llm.dto.TranscribeResponse;
import com.marketcompass.llm.groq.GroqSpeechService;
import com.marketcompass.llm.service.SpeechService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/stt")
@Slf4j
public class SpeechController {

    @Autowired(required = false) private SpeechService localSpeechService;
    @Autowired(required = false) private GroqSpeechService groqSpeechService;

    @PostMapping("/setup")
    public ResponseEntity<String> setup(@RequestBody SetupRequest request, HttpSession session) {
        session.setAttribute("jobDescription", request.getJobDescription());
        session.setAttribute("resume", request.getResume());
        log.info("Session setup complete — JD and resume stored");
        return ResponseEntity.ok("Setup complete");
    }

    @PostMapping("/transcribe")
    public ResponseEntity<TranscribeResponse> transcribe(@RequestParam("audio") MultipartFile audio,
                                                         HttpSession session) throws Exception {
        log.info("Transcribing audio: {} bytes", audio.getSize());
        String transcript = isGroq()
                ? groqSpeechService.transcribe(audio)
                : localSpeechService.transcribe(audio);
        return ResponseEntity.ok(TranscribeResponse.builder().transcript(transcript).build());
    }

    @PostMapping("/ask")
    public ResponseEntity<TranscribeResponse> transcribeAndAsk(@RequestParam("audio") MultipartFile audio,
                                                                HttpSession session) throws Exception {
        log.info("Transcribe + ask: {} bytes", audio.getSize());
        String jd     = (String) session.getAttribute("jobDescription");
        String resume = (String) session.getAttribute("resume");
        TranscribeResponse response = isGroq()
                ? groqSpeechService.transcribeAndAsk(audio, jd, resume)
                : localSpeechService.transcribeAndAsk(audio, jd, resume);
        return ResponseEntity.ok(response);
    }

    private boolean isGroq() {
        return groqSpeechService != null;
    }
}
