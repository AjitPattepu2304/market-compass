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

    private static final int MAX_RESUME_CHARS = 2200;
    private static final int MAX_JD_CHARS = 1800;
    private static final int MAX_HISTORY_MESSAGES = 2;
    private static final int MAX_HISTORY_CHARS = 600;
    private static final int MAX_QUESTION_CHARS = 1800;
    private static final int MAX_COMPLETION_TOKENS = 350;
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
        long start = System.nanoTime();
        log.info("Groq LLM question: {}", question);

        boolean personalContextNeeded = needsPersonalContext(question);
        String systemPrompt = buildSystemPrompt(
                personalContextNeeded ? jobDescription : null,
                personalContextNeeded ? resume : null
        );

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(trimHistory(history));
        messages.add(Map.of("role", "user", "content", safeText(question, MAX_QUESTION_CHARS)));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.1,
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
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                log.info("Groq LLM answered in {} ms using {} (personalContext={})",
                        elapsedMs, model, personalContextNeeded);
                return answer;

            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() == 429 && attempt < MAX_RETRIES) {
                    long waitSeconds = parseRetryAfter(e);
                    log.warn("Groq rate limit reached. Retrying in {} seconds", waitSeconds);
                    sleep(waitSeconds);
                    continue;
                }

                if (e.getStatusCode().value() == 429) {
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
        if (history == null || history.isEmpty()) return Collections.emptyList();
        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        List<Map<String, String>> recent = new ArrayList<>();
        for (int i = fromIndex; i < history.size(); i++) {
            Map<String, String> message = history.get(i);
            if (message == null) continue;
            String role = message.getOrDefault("role", "user");
            String content = safeText(message.get("content"), MAX_HISTORY_CHARS);
            if (!content.isBlank()) recent.add(Map.of("role", role, "content", content));
        }
        return recent;
    }

    private boolean needsPersonalContext(String question) {
        if (question == null || question.isBlank()) return false;
        String q = question.toLowerCase();
        String[] terms = {
                "tell me about yourself", "yourself", "your experience", "your resume",
                "your background", "your project", "your role", "your current role",
                "your work", "your skills", "your strengths", "your weakness",
                "why should we hire", "why do you want", "why this company", "why this role",
                "behavioral", "leadership", "conflict", "challenge", "difficult situation",
                "teamwork", "stakeholder", "manager", "ownership", "failure", "achievement",
                "accomplishment", "career", "walmart", "job description", " jd", "resume"
        };
        for (String term : terms) if (q.contains(term)) return true;
        return false;
    }

    private String buildSystemPrompt(String jobDescription, String resume) {
        StringBuilder prompt = new StringBuilder("""
                You are an expert interview coach helping a candidate answer interview questions.
                Answer the current question directly and correctly.
                Make the answer natural to speak in an interview.
                For technical questions, give the key idea first and a short code example only when useful.
                For behavioral questions, use concise STAR structure.
                Keep answers short: normally 2-4 short paragraphs or bullets.
                Do not give a long tutorial unless explicitly asked.
                Do not mention that you are an AI.
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
        return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars) + "\n[context truncated]";
    }

    private long parseRetryAfter(RestClientResponseException e) {
        try {
            String header = e.getResponseHeaders().getFirst("retry-after");
            if (header != null) return Math.max(1, Math.min(10, Long.parseLong(header.trim())));
        } catch (Exception ignored) { }
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
