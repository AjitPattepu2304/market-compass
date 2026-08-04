package com.marketcompass.llm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * LLMService handles Q&A interactions with Ollama/LLaMA
 * 
 * Features:
 * - Answer investment-related questions
 * - Context-aware responses (portfolio, market data)
 * - Streaming support for long responses
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {

    private final ChatClient chatClient;

    /**
     * Answer a user question with investment context
     * 
     * @param question User's investment question
     * @param context Optional context (portfolio data, market info)
     * @return LLM-generated answer
     */
    public String answerQuestion(String question, String context) {
        log.info("Processing question: {}", question);

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(question, context);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .getResult()
                    .getOutput()
                    .getContent();

            log.info("Generated response successfully");
            return response;

        } catch (Exception e) {
            log.error("Error processing question: {}", question, e);
            return "Unable to process your question at this time. Please try again later.";
        }
    }

    /**
     * Answer without additional context
     */
    public String answerQuestion(String question) {
        return answerQuestion(question, "");
    }

    /**
     * Build system prompt with investment advisor persona
     */
    private String buildSystemPrompt() {
        return """You are an expert investment advisor with deep knowledge of:
- Stock market fundamentals and technical analysis
- ETFs and diversification strategies
- Dividend investing and income generation
- Risk management and portfolio construction
- Semiconductor sector trends
- Market psychology and behavioral finance

Provide clear, actionable advice based on the user's questions.
Always emphasize risk management and the importance of doing your own research.
Disclaim that this is not financial advice and should not be taken as a substitute for professional financial advice.

Keep responses concise but informative (2-3 paragraphs max).
""";
    }

    /**
     * Build user prompt with optional context
     */
    private String buildUserPrompt(String question, String context) {
        StringBuilder prompt = new StringBuilder();
        
        if (context != null && !context.isBlank()) {
            prompt.append("Context:\n");
            prompt.append(context);
            prompt.append("\n\n");
        }
        
        prompt.append("Question: ");
        prompt.append(question);
        
        return prompt.toString();
    }

}
