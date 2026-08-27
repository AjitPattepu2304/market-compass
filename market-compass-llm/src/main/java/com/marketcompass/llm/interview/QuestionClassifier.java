package com.marketcompass.llm.interview;

import org.springframework.stereotype.Service;

import java.util.Locale;

/** Fast deterministic routing before an LLM call. Keeps obvious follow-ups tied to the live topic. */
@Service
public class QuestionClassifier {
    public QuestionType classify(String question, String previousQuestion) {
        String q = normalize(question);
        if (q.isBlank()) return QuestionType.UNKNOWN;

        if (isFollowUp(q, previousQuestion)) return QuestionType.FOLLOW_UP;
        if (containsAny(q, "write code", "implement", "coding", "leetcode", "algorithm", "code")
                || containsAny(q, "array", "linked list", "binary tree", "graph", "dynamic programming")) {
            return QuestionType.CODING;
        }
        if (containsAny(q, "design a system", "system design", "architecture", "scalability", "scale to",
                "distributed system", "high availability")) {
            return QuestionType.SYSTEM_DESIGN;
        }
        if (containsAny(q, "tell me about a time", "describe a time", "conflict", "leadership",
                "failure", "challenge", "difficult stakeholder", "team disagreement")) {
            return QuestionType.BEHAVIORAL;
        }
        if (containsAny(q, "have you used", "have you worked", "what did you build", "what did you own",
                "your experience", "in your project", "at walmart", "what was your role")) {
            return QuestionType.EXPERIENCE;
        }
        return QuestionType.TECHNICAL;
    }

    private boolean isFollowUp(String q, String previousQuestion) {
        if (previousQuestion == null || previousQuestion.isBlank()) return false;
        return q.length() < 90 && containsAny(q, "why", "how", "what happened", "what did you do",
                "then what", "and then", "what about", "can you explain", "more about", "what if", "how so");
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
