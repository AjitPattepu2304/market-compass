package com.marketcompass.llm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import org.springframework.context.annotation.Profile;

@Service
@Profile("!groq")
@Slf4j
@RequiredArgsConstructor
public class LLMService {

    private final ChatClient chatClient;

    public String answerQuestion(String question) {
        return answerQuestion(question, null, null);
    }

    public String answerQuestion(String question, String jobDescription, String resume) {
        log.info("Processing question: {}", question);
        try {
            String system = buildSystemPrompt(jobDescription, resume);
            String response = chatClient.prompt()
                    .system(system)
                    .user("Interview question: " + question)
                    .call()
                    .content();
            log.info("Generated response successfully");
            return response;
        } catch (Exception e) {
            log.error("Error processing question: {}", question, e);
            return "Unable to process your question at this time. Please try again later.";
        }
    }

    private String buildSystemPrompt(String jobDescription, String resume) {
        String base = """
                You are an expert interview coach helping a candidate ace their interview.
                Your job is to answer ANY question the interviewer asks — technical, behavioral, or conceptual.

                Rules:
                - Always give a complete, correct answer regardless of whether the topic appears in the JD or resume.
                - For technical questions (code, algorithms, system design, frameworks), give a clear explanation with a code example if helpful.
                - For behavioral questions, use the STAR method (Situation, Task, Action, Result).
                - Keep answers concise and interview-ready — the candidate should be able to speak it naturally.
                - Do not refuse or deflect any question. If the topic is outside the JD, still answer it fully.
                - Do not mention that you are an AI.
                - Format code in markdown code blocks with the correct language tag.
                """;

        if (jobDescription == null && resume == null) return base;

        return base + String.format("""

                Use the context below to personalize answers where relevant (e.g. connect examples to the candidate's experience).
                But always answer the question fully even if the topic is not in the JD or resume.

                JOB DESCRIPTION:
                %s

                CANDIDATE RESUME:
                %s
                """,
                jobDescription != null ? jobDescription : "Not provided",
                resume != null ? resume : "Not provided");
    }
}
