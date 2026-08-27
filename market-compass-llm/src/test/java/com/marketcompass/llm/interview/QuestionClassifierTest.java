package com.marketcompass.llm.interview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionClassifierTest {
    private final QuestionClassifier classifier = new QuestionClassifier();

    @Test
    void detectsTechnicalQuestion() {
        assertEquals(QuestionType.TECHNICAL,
                classifier.classify("What is the difference between Kafka and RabbitMQ?", ""));
    }

    @Test
    void detectsExperienceQuestion() {
        assertEquals(QuestionType.EXPERIENCE,
                classifier.classify("Have you worked with Cassandra?", ""));
    }

    @Test
    void keepsShortFollowUpConnectedToPreviousQuestion() {
        assertEquals(QuestionType.FOLLOW_UP,
                classifier.classify("Why?", "How did you migrate the database?"));
    }

    @Test
    void detectsCodingQuestion() {
        assertEquals(QuestionType.CODING,
                classifier.classify("Can you implement an LRU cache?", ""));
    }
}
