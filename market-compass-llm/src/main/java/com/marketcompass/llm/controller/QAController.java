package com.marketcompass.llm.controller;

import com.marketcompass.llm.dto.QuestionRequest;
import com.marketcompass.llm.dto.AnswerResponse;
import com.marketcompass.llm.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for LLM-powered Q&A
 * 
 * Endpoints:
 * - POST /api/llm/qa - Ask a question
 * - POST /api/llm/qa/contextual - Ask with portfolio context
 */
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@Slf4j
public class QAController {

    private final LLMService llmService;

    /**
     * Ask a question about investments
     * 
     * @param request Question request
     * @return Answer from LLM
     */
    @PostMapping("/qa")
    public ResponseEntity<AnswerResponse> askQuestion(@RequestBody QuestionRequest request) {
        log.info("Received question: {}", request.getQuestion());
        
        String answer = llmService.answerQuestion(request.getQuestion());
        
        AnswerResponse response = AnswerResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .model("ollama/llama2")
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Ask a question with additional context
     * 
     * @param request Question with context
     * @return Answer from LLM
     */
    @PostMapping("/qa/contextual")
    public ResponseEntity<AnswerResponse> askQuestionWithContext(@RequestBody QuestionRequest request) {
        log.info("Received contextual question: {}", request.getQuestion());
        
        String answer = llmService.answerQuestion(request.getQuestion(), request.getContext());
        
        AnswerResponse response = AnswerResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .context(request.getContext())
                .model("ollama/llama2")
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("LLM service is running");
    }

}
