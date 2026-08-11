package com.marketcompass.llm.groq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Profile("groq")
@Slf4j
public class GroqLLMService {

    // Keep the prompt small enough for the free-tier TPM limit.
    private static final int MAX_RESUME_CHARS = 7000;
    private static final int MAX_JD_CHARS = 5000;
    private static final int MAX_HISTORY_MESSAGES = 2;
    private static final int MAX_HISTORY_CHARS = 1200;
    private static final int MAX_COMPLETION_TOKENS = 650;
    private static final int MAX_RETRIES = 1;

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.llm-model:llama-3.1-8b-instant}")
    private String model;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .build();

    public String answerQuestion(String question, String jobDescription, String resume) {
        return answerQuestion(question, jobDescription, resume, List.of());
    }

    public String answerQuestion(String question, String jobDescription, String resume,
                                 List<Map<String, String>> history) {
        log.info("Groq LLM question: {}", question);

        String systemPrompt = buildSystemPrompt(jobDescription, resume);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(trimHistory(history));
        messages.add(Map.of("role", "user", "content", safeText(question, 2500)));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.2,
                "max_completion_tokens", MAX_COMPLETION_TOKENS
        );

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<?, ?> response = restClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .body(body)
                        .retrieve()
                        .body(Map.class);

                String answer = extractAnswer(response);
                log.info("Groq LLM answered successfully using {}", model);
                return answer;

            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() == 429 && attempt < MAX_RETRIES) {
                    long waitSeconds = parseRetryAfter(e);
                    log.warn("Groq rate limit reached. Retrying in {} seconds", waitSeconds);
                    sleep(waitSeconds);
                    continue;
                }

                if (e.getStatusCode().value() == 429) {
                    log.warn("Groq rate limit still active after retry: {}", e.getResponseBodyAsString());
                    return "Groq is temporarily rate-limited. Please wait a few seconds and try again.";
                }

                log.error("Groq request failed: HTTP {} - {}", e.getStatusCode().value(),
                        e.getResponseBodyAsString());
                return "Unable to generate an answer right now. Please try the question again.";

            } catch (Exception e) {
                log.error("Unexpected Groq LLM error", e);
                return "Unable to generate an answer right now. Please try the question again.";
            }
        }

        return "Unable to generate an answer right now. Please try the question again.";
    }

    private String extractAnswer(Map<?, ?> response) {
        try {
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "Unable to generate an answer right now. Please try the question again.";
            }

            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            String answer = (String) message.get("content");

            return answer == null || answer.isBlank()
                    ? "Unable to generate an answer right now. Please try the question again."
                    : answer.trim();
        } catch (Exception e) {
            log.error("Failed to parse Groq response", e);
            return "Unable to generate an answer right now. Please try the question again.";
        }
    }

    private List<Map<String, String>> trimHistory(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        List<Map<String, String>> recent = new ArrayList<>();

        for (int i = fromIndex; i < history.size(); i++) {
            Map<String, String> message = history.get(i);
            if (message == null) continue;

            String role = message.getOrDefault("role", "user");
            String content = safeText(message.get("content"), MAX_HISTORY_CHARS);
            if (!content.isBlank()) {
                recent.add(Map.of("role", role, "content", content));
            }
        }

        return recent;
    }

    private String buildSystemPrompt(String jobDescription, String resume) {
        StringBuilder prompt = new StringBuilder("""
                You are an expert interview coach helping a candidate answer live interview questions.

                Rules:
                - Answer the current interviewer question directly and correctly.
                - Make the answer sound natural when spoken by a candidate.
                - For technical questions, explain the key idea first and give a short code example only when useful.
                - For behavioral questions, use a concise STAR-style answer.
                - Do not give long tutorials unless the interviewer explicitly asks for detail.
                - Keep the answer concise: normally 2-5 short paragraphs or bullets.
                - Do not mention that you are an AI.
                - Never repeat the resume or job description unnecessarily.
                """);

        if (jobDescription != null && !jobDescription.isBlank()) {
            prompt.append("\nJOB DESCRIPTION (use only when relevant):\n")
                    .append(safeText(jobDescription, MAX_JD_CHARS));
        }

        if (resume != null && !resume.isBlank()) {
            prompt.append("\n\nCANDIDATE RESUME (use only when relevant):\n")
                    .append(safeText(resume, MAX_RESUME_CHARS));
        }

        return prompt.toString();
    }

    private String safeText(String value, int maxChars) {
        if (value == null || value.isBlank()) return "";
        String trimmed = value.trim();
        if (trimmed.length() <= maxChars) return trimmed;
        return trimmed.substring(0, maxChars) + "\n[context truncated]";
    }

    private long parseRetryAfter(RestClientResponseException e) {
        try {
            String header = e.getResponseHeaders().getFirst("retry-after");
            if (header != null) {
                return Math.max(1, Math.min(10, Long.parseLong(header.trim())));
            }
        } catch (Exception ignored) {
            // Fall through to the default wait.
        }
        return 3;
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
