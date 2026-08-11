package com.marketcompass.llm.controller;

import com.marketcompass.llm.dto.AnswerResponse;
import com.marketcompass.llm.dto.QuestionRequest;
import com.marketcompass.llm.groq.GroqLLMService;
import com.marketcompass.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/llm")
@Slf4j
public class QAController {

    @Autowired(required = false) private LLMService llmService;
    @Autowired(required = false) private GroqLLMService groqLLMService;

    @PostMapping("/qa")
    public ResponseEntity<AnswerResponse> askQuestion(@RequestBody QuestionRequest request) {
        log.info("Received question: {}", request.getQuestion());
        String answer = answer(request.getQuestion(), null, null);
        return ResponseEntity.ok(AnswerResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .model(modelName())
                .build());
    }

    @PostMapping("/qa/contextual")
    public ResponseEntity<AnswerResponse> askQuestionWithContext(@RequestBody QuestionRequest request) {
        log.info("Received contextual question: {}", request.getQuestion());
        String answer = answer(request.getQuestion(), null, request.getContext());
        return ResponseEntity.ok(AnswerResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .context(request.getContext())
                .model(modelName())
                .build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("LLM service is running — mode: " + modelName());
    }

    private String answer(String question, String jd, String context) {
        if (groqLLMService != null) return groqLLMService.answerQuestion(question, jd, context);
        return llmService.answerQuestion(question, jd, context);
    }

    private String modelName() {
        return groqLLMService != null ? "groq/llama-3.3-70b-versatile" : "ollama/llama3.2:3b";
    }
}
