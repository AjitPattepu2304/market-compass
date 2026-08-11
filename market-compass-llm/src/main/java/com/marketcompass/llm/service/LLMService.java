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
