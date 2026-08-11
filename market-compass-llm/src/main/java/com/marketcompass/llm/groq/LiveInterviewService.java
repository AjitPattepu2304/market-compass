package com.marketcompass.llm.groq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@Profile("groq")
@Slf4j
public class LiveInterviewService {
    @Value("${groq.api-key}") private String apiKey;
    @Value("${groq.llm-model:llama-3.1-8b-instant}") private String model;

    private final RestClient client = RestClient.builder().baseUrl("https://api.groq.com/openai/v1").build();

    public String answer(String conversation, String jd, String resume, List<Map<String,String>> history) {
        String system = """
                You are helping an experienced software engineer answer a live technical interview.

                The input is a rolling transcript containing both interviewer and candidate speech.
                Identify the LATEST interviewer question or follow-up. Do not answer the candidate's own statements.
                Use the previous discussion to understand short follow-ups such as why, how, what did you do, or what happened next.

                ANSWER STYLE:
                - First person when speaking from candidate experience.
                - Conversational, confident, practical, and professional.
                - Sound like an experienced engineer speaking aloud, not a textbook or generic AI.
                - Give the direct answer first, then the practical explanation.
                - Use real candidate experience from the supplied resume when relevant.
                - Explain why a technical decision was made and mention trade-offs when useful.
                - Keep normally to 2-4 short paragraphs or concise bullets.
                - For behavioral questions, answer naturally using the candidate's actual experience.
                - Never invent a project, technology, metric, responsibility, or achievement.
                - If the transcript is ambiguous, answer the most recent clear interviewer question and do not mention the ambiguity.

                JOB DESCRIPTION:
                """ + safe(jd, 3000) + """

                CANDIDATE RESUME / EXPERIENCE:
                """ + safe(resume, 5500);

        List<Map<String,String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        if (history != null) {
            int start = Math.max(0, history.size() - 4);
            for (int i = start; i < history.size(); i++) {
                Map<String,String> h = history.get(i);
                if (h != null && h.get("content") != null) {
                    messages.add(Map.of("role", h.getOrDefault("role", "user"), "content", safe(h.get("content"), 900)));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", "ROLLING LIVE TRANSCRIPT:\n" + safe(conversation, 5000)));

        Map<String,Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.25,
                "max_completion_tokens", 420
        );

        try {
            Map<?,?> response = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body).retrieve().body(Map.class);
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "Unable to generate an answer right now.";
            Map<?,?> choice = (Map<?,?>) choices.get(0);
            Map<?,?> message = (Map<?,?>) choice.get("message");
            Object content = message.get("content");
            return content == null ? "Unable to generate an answer right now." : content.toString().trim();
        } catch (Exception e) {
            log.error("Live interview Groq request failed", e);
            return "Unable to generate an answer right now. Please try Answer Now again.";
        }
    }

    private String safe(String value, int max) {
        if (value == null || value.isBlank()) return "Not provided";
        String s = value.trim();
        return s.length() <= max ? s : s.substring(0, max) + "\n[truncated]";
    }
}
