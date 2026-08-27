package com.marketcompass.llm.interview;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewGuidanceService {
    private final QuestionClassifier questionClassifier;
    private final FollowUpPredictor followUpPredictor;

    public InterviewAnalysis analyze(String transcript, List<java.util.Map<String, String>> history) {
        String previousQuestion = previousQuestion(history);
        String question = extractLatestQuestion(transcript);
        QuestionType type = questionClassifier.classify(question, previousQuestion);
        String topic = inferTopic(question, previousQuestion);
        return InterviewAnalysis.builder()
                .questionType(type)
                .question(question)
                .topic(topic)
                .likelyFollowUps(followUpPredictor.predict(type, topic, ""))
                .build();
    }

    private String previousQuestion(List<java.util.Map<String, String>> history) {
        if (history == null) return "";
        for (int i = history.size() - 1; i >= 0; i--) {
            var item = history.get(i);
            if (item != null && "user".equals(item.get("role"))) {
                return extractLatestQuestion(item.get("content"));
            }
        }
        return "";
    }

    private String extractLatestQuestion(String transcript) {
        if (transcript == null) return "";
        String normalized = transcript.replaceAll("\\s+", " ").trim();
        int q = normalized.lastIndexOf('?');
        if (q >= 0) {
            int start = Math.max(normalized.lastIndexOf('.', q), normalized.lastIndexOf('!', q));
            return normalized.substring(start + 1, q + 1).trim();
        }
        return normalized;
    }

    private String inferTopic(String question, String previousQuestion) {
        String q = question == null ? "" : question;
        if (q.isBlank()) return "current interview topic";
        String lower = q.toLowerCase();
        String[] known = {"cassandra", "kafka", "java", "spring boot", "microservices", "aws",
                "azure", "kubernetes", "docker", "postgres", "sql", "system design", "migration",
                "distributed systems", "rest api", "performance", "scalability"};
        for (String topic : known) if (lower.contains(topic)) return topic;
        if (previousQuestion != null && !previousQuestion.isBlank() &&
                (lower.startsWith("why") || lower.startsWith("how") || lower.startsWith("what if") || lower.startsWith("and "))) {
            return previousQuestion.length() > 80 ? previousQuestion.substring(0, 80) : previousQuestion;
        }
        return "current interview topic";
    }
}
