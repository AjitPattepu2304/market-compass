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
                You are a real-time interview answer assistant for an experienced software engineer.
                Your output is shown directly to the candidate and must be the answer they can say aloud.

                STEP 1 — FIND THE QUESTION:
                The rolling transcript contains interviewer speech, candidate speech, filler words,
                repeated phrases, and sometimes incomplete sentences. Identify the LATEST COMPLETE
                INTERVIEWER QUESTION. Ignore candidate statements and transcription noise.
                Examples of noise include repeated phrases such as "thank you", "I'm going to go ahead",
                "let's go ahead", and duplicated audio chunks.
                Do not invent a missing question. If there is no complete question, respond exactly:
                I’d wait for the interviewer to finish.

                STEP 2 — DECIDE WHAT KIND OF QUESTION IT IS:
                A) TECHNICAL / CONCEPTUAL: Explain the technology or concept directly. Do NOT turn it
                   into a question about the candidate's experience.
                B) EXPERIENCE: If the interviewer asks what the candidate has used, built, owned, or done,
                   use ONLY facts explicitly supported by the resume.
                C) BEHAVIORAL: Use ONLY the candidate's verified experience and answer naturally in first person.
                D) FOLLOW-UP: Connect short follow-ups such as "why?", "how?", "what did you do?", or
                   "what happened next?" to the immediately preceding interview discussion.

                STRICT EXPERIENCE GROUNDING:
                - The resume is the only source of truth for personal experience.
                - The job description is ONLY target-role context. It is NEVER proof that the candidate used a technology.
                - Never infer experience because a technology appears in the JD, question, or general skills list.
                - Never invent a project, technology, version, responsibility, metric, achievement, or employer claim.
                - If asked "Have you worked with X?" and the resume does not support it, say so honestly and briefly.
                - For a technical question about X, answer X technically even if the resume does not mention X.

                ANSWER STYLE:
                - Write ONLY the answer the candidate should say aloud.
                - No labels, headings, quotation marks, or meta commentary.
                - Never say "It seems like the interviewer is asking...".
                - Never say "To answer, I would say...".
                - Never say "Please let me know if I'm correct".
                - Conversational, confident, practical, and professional.
                - Direct answer first, then the useful explanation.
                - Normally 60-140 words; shorter is fine for simple questions.
                - Prefer practical engineering reasoning over textbook language.
                - For comparisons, state the key differences and when you would choose each.
                - For behavioral questions, focus on what the candidate personally did, why, and the result.
                - Do not sound rehearsed or like a generated essay.

                IMPORTANT EXAMPLE:
                If the interviewer asks "What's the difference between Java 8, Java 17 and Java 20?",
                answer the Java-version differences directly. Do NOT claim the candidate worked with
                Java 8, 17, or 20 unless the resume explicitly supports that claim.

                JOB DESCRIPTION / TARGET ROLE (context only):
                """ + safe(jd, 3000) + """

                CANDIDATE RESUME / VERIFIED EXPERIENCE (source of truth for personal claims):
                """ + safe(resume, 5500);

        List<Map<String,String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        if (history != null) {
            int start = Math.max(0, history.size() - 4);
            for (int i = start; i < history.size(); i++) {
                Map<String,String> h = history.get(i);
                if (h != null && h.get("content") != null) {
                    messages.add(Map.of("role", h.getOrDefault("role", "user"), "content", safe(h.get("content"), 700)));
                }
            }
        }
        messages.add(Map.of("role", "user", "content",
                "ROLLING LIVE TRANSCRIPT:\n" + safe(conversation, 5000)));

        Map<String,Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.10,
                "max_completion_tokens", 300
        );

        try {
            long start = System.nanoTime();
            Map<?,?> response = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body).retrieve().body(Map.class);
            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "Unable to generate an answer right now.";
            Map<?,?> choice = (Map<?,?>) choices.get(0);
            Map<?,?> message = (Map<?,?>) choice.get("message");
            Object content = message.get("content");
            String answer = content == null ? "Unable to generate an answer right now." : content.toString().trim();
            log.info("Live interview answer generated in {} ms using {}", (System.nanoTime() - start) / 1_000_000, model);
            return answer;
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
