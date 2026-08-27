package com.marketcompass.llm.interview;

import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Fast deterministic routing before an LLM call.
 * Recognizes common software-engineering concepts while keeping
 * experience/behavioral/follow-up routing separate from technical questions.
 */
@Service
public class QuestionClassifier {
    public QuestionType classify(String question, String previousQuestion) {
        String q = normalize(question);
        if (q.isBlank()) return QuestionType.UNKNOWN;

        if (isFollowUp(q, previousQuestion)) return QuestionType.FOLLOW_UP;

        if (containsAny(q,
                "write code", "implement", "coding", "leetcode", "algorithm", "code this",
                "solve this", "time complexity", "space complexity",
                "array", "arrays", "linked list", "stack", "queue", "deque",
                "hashmap", "hash map", "hashset", "hash set", "hashtable", "hash table",
                "binary tree", "binary search tree", "heap", "priority queue", "trie",
                "graph", "graphs", "dynamic programming", "recursion", "backtracking",
                "sorting", "searching", "two pointer", "sliding window")) {
            return QuestionType.CODING;
        }

        if (containsAny(q,
                "design a system", "system design", "architecture", "scalability", "scale to",
                "distributed system", "distributed systems", "high availability", "fault tolerance",
                "load balancing", "load balancer", "caching strategy", "event driven architecture",
                "event-driven architecture", "design an api", "design a service")) {
            return QuestionType.SYSTEM_DESIGN;
        }

        if (containsAny(q,
                "tell me about a time", "describe a time", "conflict", "leadership",
                "failure", "challenge", "difficult stakeholder", "team disagreement",
                "disagreement with", "mistake you made", "biggest mistake", "strength", "weakness")) {
            return QuestionType.BEHAVIORAL;
        }

        if (containsAny(q,
                "have you used", "have you worked", "what did you build", "what did you own",
                "your experience", "in your project", "at walmart", "what was your role",
                "tell me about your experience", "how did you use", "where did you use")) {
            return QuestionType.EXPERIENCE;
        }

        // Common software-engineering concepts should always be treated as technical.
        if (containsAny(q,
                "java", "jvm", "spring", "spring boot", "kotlin", "microservice", "microservices",
                "rest", "restful", "api", "http", "https", "json", "oauth", "jwt",
                "kafka", "rabbitmq", "cassandra", "postgres", "postgresql", "mysql", "oracle",
                "sql", "nosql", "database", "databases", "index", "indexes", "transaction",
                "acid", "cap theorem", "docker", "kubernetes", "k8s", "aws", "azure", "gcp",
                "thread", "threads", "multithreading", "concurrency", "synchronization", "deadlock",
                "garbage collection", "garbage collector", "heap", "stack", "polymorphism",
                "inheritance", "encapsulation", "abstraction", "interface", "class", "object",
                "dependency injection", "solid", "design pattern", "observer pattern", "factory pattern",
                "singleton", "exception handling", "stream api", "lambda", "generics", "collections",
                "optional", "completablefuture", "async", "synchronous", "asynchronous", "thread pool",
                "memory management", "serialization", "deserialization", "unit test", "testing",
                "git", "ci/cd", "continuous integration", "continuous deployment")) {
            return QuestionType.TECHNICAL;
        }

        // If the transcript is a normal non-empty question, the safest default is technical.
        return QuestionType.TECHNICAL;
    }

    private boolean isFollowUp(String q, String previousQuestion) {
        if (previousQuestion == null || previousQuestion.isBlank()) return false;
        return q.length() < 90 && containsAny(q, "why", "how", "what happened", "what did you do",
                "then what", "and then", "what about", "can you explain", "more about", "what if", "how so");
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
