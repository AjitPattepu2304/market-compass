package com.marketcompass.llm.groq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Profile("groq")
@Slf4j
public class GroqLLMService {

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.llm-model:llama-3.3-70b-versatile}")
    private String model;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .build();

    public String answerQuestion(String question, String jobDescription, String resume) {
        log.info("Groq LLM question: {}", question);

        String systemPrompt = buildSystemPrompt(jobDescription, resume);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Interview question: " + question)
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        try {
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            String answer = (String) message.get("content");
            log.info("Groq LLM answered successfully");
            return answer;
        } catch (Exception e) {
            log.error("Failed to parse Groq LLM response", e);
            return "Unable to generate an answer at this time.";
        }
    }

    private String buildSystemPrompt(String jobDescription, String resume) {
        String base = """
                You are an interview practice assistant.
                Answer the interviewer's question directly and concisely.

                Rules:
                - Do not invent information that was not provided.
                - Do not assume the candidate's job role unless it is provided in the context.
                - Do not add unrelated technologies or frameworks.
                - If the question asks for code, provide the simplest correct example.
                - If the question is ambiguous, state the assumption briefly.
                - For technical questions, explain the answer in an interview-ready way.
                - Prefer practical examples over lengthy explanations.
                - Keep the answer concise enough for the candidate to understand and respond naturally.
                - Do not mention that you are an AI.
                """;

        if (jobDescription == null && resume == null) return base;

        return base + String.format("""

                JOB DESCRIPTION:
                %s

                CANDIDATE RESUME:
                %s

                Always answer based on the resume above. Tailor your answers to the job description.
                """,
                jobDescription != null ? jobDescription : "Not provided",
                resume != null ? resume : "Not provided");
    }
}
