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

        // Coding means the interviewer is asking the candidate to implement/solve something,
        // not merely explain a data structure or algorithm concept.
        if (containsAny(q,
                "write code", "write a program", "implement", "coding question", "leetcode",
                "code this", "solve this", "solve the problem", "provide code", "show me the code",
                "write the code", "write a solution", "code an algorithm")) {
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

        // Common software-engineering and computer-science concepts are technical.
        // This includes questions such as "What is HashMap?" and "How does Kafka work?".
        if (containsAny(q,
                "java", "jvm", "spring", "spring boot", "kotlin", "microservice", "microservices",
                "rest", "restful", "api", "http", "https", "json", "oauth", "jwt",
                "kafka", "rabbitmq", "cassandra", "postgres", "postgresql", "mysql", "oracle",
                "sql", "nosql", "database", "databases", "index", "indexes", "transaction",
                "acid", "cap theorem", "docker", "kubernetes", "k8s", "aws", "azure", "gcp",
                "thread", "threads", "multithreading", "concurrency", "synchronization", "deadlock",
                "garbage collection", "garbage collector", "heap", "stack", "polymorphism",
                "inheritance", "encapsulation", "abstraction", "interface", "class", "object",
                "hashmap", "hash map", "hashset", "hash set", "hashtable", "hash table",
                "array", "arrays", "linked list", "linked lists", "queue", "queues", "deque",
                "binary tree", "binary search tree", "heap", "priority queue", "trie", "graph", "graphs",
                "dynamic programming", "recursion", "backtracking", "sorting", "searching",
                "time complexity", "space complexity", "big o", "data structure", "data structures",
                "dependency injection", "solid", "design pattern", "observer pattern", "factory pattern",
                "singleton", "exception handling", "stream api", "lambda", "generics", "collections",
                "optional", "completablefuture", "async", "synchronous", "asynchronous", "thread pool",
                "memory management", "serialization", "deserialization", "unit test", "testing",
                "git", "ci/cd", "continuous integration", "continuous deployment")) {
            return QuestionType.TECHNICAL;
        }

        // A normal non-empty interview question with no special signal defaults to technical.
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
