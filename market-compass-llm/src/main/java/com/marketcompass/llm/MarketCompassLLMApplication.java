package com.marketcompass.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MarketCompass LLM Application
 * 
 * Standalone Spring Boot application for LLM-powered Q&A service.
 * Uses Ollama/LLaMA for investment advice and portfolio analysis.
 */
@SpringBootApplication
public class MarketCompassLLMApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketCompassLLMApplication.class, args);
    }

}
