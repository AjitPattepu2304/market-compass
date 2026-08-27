package com.marketcompass.llm.interview;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lightweight first-pass follow-up predictor. It deliberately uses the current
 * topic and answer rather than pretending to know the interviewer's intent.
 */
@Service
public class FollowUpPredictor {
    public List<String> predict(QuestionType type, String topic, String answer) {
        String t = topic == null ? "this topic" : topic;
        return switch (type) {
            case EXPERIENCE, BEHAVIORAL -> List.of(
                    "What was the biggest challenge with " + t + "?",
                    "What did you personally do?",
                    "What was the result or measurable impact?");
            case TECHNICAL -> List.of(
                    "Why would you choose this approach?",
                    "What are the trade-offs?",
                    "What happens under failure or high load?");
            case CODING -> List.of(
                    "What is the time and space complexity?",
                    "What edge cases would you handle?",
                    "Can you improve or modify the approach?");
            case SYSTEM_DESIGN -> List.of(
                    "How would you scale this further?",
                    "How would you handle failures and retries?",
                    "What consistency or availability trade-off would you make?");
            case FOLLOW_UP -> List.of(
                    "Can you explain the implementation detail?",
                    "What was your personal contribution?",
                    "What would you change today?");
            default -> List.of(
                    "Why did you choose that approach?",
                    "Can you give a practical example?",
                    "What were the trade-offs?");
        };
    }
}
