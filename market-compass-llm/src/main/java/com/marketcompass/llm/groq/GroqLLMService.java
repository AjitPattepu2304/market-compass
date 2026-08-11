package com.marketcompass.llm.groq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Service
@Profile("groq")
@Slf4j
public class GroqLLMService {

    // Keep enough context to make answers personal without sending the entire
    // resume/JD on every request. Relevant resume sections are selected below.
    private static final int MAX_RESUME_CHARS = 5200;
    private static final int MAX_JD_CHARS = 2600;
    private static final int MAX_HISTORY_MESSAGES = 4;
    private static final int MAX_HISTORY_CHARS = 700;
    private static final int MAX_QUESTION_CHARS = 1800;
    private static final int MAX_COMPLETION_TOKENS = 450;
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

        String relevantResume = selectRelevantResumeContext(question, resume);
        String relevantJd = safeText(jobDescription, MAX_JD_CHARS);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                buildSystemPrompt(relevantJd, relevantResume)));
        messages.addAll(trimHistory(history));
        messages.add(Map.of("role", "user", "content", safeText(question, MAX_QUESTION_CHARS)));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.25,
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
                log.info("Groq LLM answered in {} ms using {} (resumeContext={} chars, jdContext={} chars)",
                        elapsedMs, model, relevantResume.length(), relevantJd.length());
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

    private String buildSystemPrompt(String jobDescription, String resume) {
        StringBuilder prompt = new StringBuilder("""
                You are helping the candidate answer a live technical interview.

                The candidate is an experienced software engineer. Your answers must sound like
                the candidate is speaking from real engineering experience, not like a textbook,
                documentation page, Google result, or generic AI response.

                VOICE:
                - Speak in first person when answering from the candidate's experience.
                - Sound conversational, confident, and professional.
                - Prefer practical engineering examples over textbook definitions.
                - Use the candidate's real projects, technologies, responsibilities, and achievements when relevant.
                - Explain why a decision was made, not only what technology was used.
                - Mention trade-offs when useful.
                - Avoid unnecessary introductions such as "Sure, here's an explanation."
                - Avoid generic phrases such as "In today's rapidly evolving technological landscape."
                - Do not over-explain basic concepts.
                - Do not sound rehearsed or overly polished.
                - Never invent experience, projects, technologies, metrics, responsibilities, or achievements.

                TECHNICAL QUESTIONS:
                1. Give the direct answer first.
                2. Explain how it applies in real engineering work.
                3. Use the candidate's actual experience when relevant.
                4. Give a short practical example or code snippet only when useful.
                5. If the question asks for a comparison, explain the trade-off and when you would choose each option.

                BEHAVIORAL QUESTIONS:
                - Answer in first person using the candidate's actual experience.
                - Use STAR naturally, without labeling the sections.
                - Focus on what the candidate personally did, why, and what the outcome was.

                FOLLOW-UP QUESTIONS:
                - Treat the previous interviewer questions and answers as active conversation context.
                - If the interviewer asks "why?", "how?", "what did you do?", "what happened next?", or similar,
                  connect the answer to the immediately preceding discussion.
                - Do not restart with a generic definition when a follow-up is clearly referring to the previous topic.

                ANSWER LENGTH:
                - Normally 2-4 short paragraphs or concise bullets.
                - Sound like something an experienced engineer could actually say aloud.
                - Be concise unless the interviewer explicitly asks for more detail.

                IMPORTANT:
                - Candidate context below is factual source material. Use it to personalize answers.
                - If the context does not support a claim, do not make the claim.
                """);

        if (!jobDescription.isBlank()) {
            prompt.append("\n\nJOB DESCRIPTION / TARGET ROLE:\n")
                    .append(jobDescription);
        }
        if (!resume.isBlank()) {
            prompt.append("\n\nRELEVANT CANDIDATE EXPERIENCE:\n")
                    .append(resume);
        }
        return prompt.toString();
    }

    /**
     * Select the most useful resume paragraphs for the current question while
     * always retaining the beginning of the resume (usually summary/role/skills).
     */
    private String selectRelevantResumeContext(String question, String resume) {
        if (resume == null || resume.isBlank()) return "";
        if (resume.length() <= MAX_RESUME_CHARS) return resume.trim();

        String[] paragraphs = resume.split("\\n\\s*\\n|(?<=\\.)\\s{2,}");
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : q.split("[^a-zA-Z0-9+#.-]+")) {
            if (token.length() >= 3) keywords.add(token);
        }

        List<String> scored = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.isBlank()) continue;
            int score = 0;
            String lower = p.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (lower.contains(keyword)) score += keyword.length() >= 6 ? 3 : 1;
            }
            if (lower.contains("walmart") || lower.contains("software engineer")) score += 2;
            scored.add(String.format(Locale.ROOT, "%05d|%s", score, p));
        }

        scored.sort(Comparator.reverseOrder());
        StringBuilder result = new StringBuilder();

        // Keep the first part because it commonly contains the candidate profile/skills.
        String first = resume.substring(0, Math.min(1200, resume.length())).trim();
        result.append(first);

        for (String scoredParagraph : scored) {
            String paragraph = scoredParagraph.substring(6).trim();
            if (paragraph.equals(first)) continue;
            if (result.length() + paragraph.length() + 2 > MAX_RESUME_CHARS) break;
            result.append("\n\n").append(paragraph);
        }
        return result.toString();
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

    private String extractAnswer(Map<?, ?> response) {
        try {
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "Unable to generate an answer right now. Please try the question again.";
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
