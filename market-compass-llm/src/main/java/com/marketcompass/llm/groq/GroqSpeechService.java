package com.marketcompass.llm.groq;

import com.marketcompass.llm.dto.TranscribeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@Profile("groq")
@Slf4j
@RequiredArgsConstructor
public class GroqSpeechService {

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.stt-model:whisper-large-v3}")
    private String sttModel;

    private final GroqLLMService groqLLMService;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.groq.com")
            .build();

    /**
     * Generic transcription endpoint used outside an interview session.
     */
    public String transcribe(MultipartFile audio) throws Exception {
        return transcribe(audio, "", "");
    }

    /**
     * Interview-aware transcription. The Groq Whisper prompt is deliberately
     * populated with software-engineering vocabulary because ASR errors such
     * as "Fetchman" for "HashMap" are especially damaging in a technical interview.
     */
    public String transcribe(MultipartFile audio, String jobDescription, String resume) throws Exception {
        if (audio == null || audio.isEmpty()) {
            log.warn("Groq STT: empty audio upload");
            return "";
        }

        byte[] audioBytes = audio.getBytes();
        String contentType = audio.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "audio/webm";
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            mediaType = MediaType.parseMediaType("audio/webm");
            contentType = "audio/webm";
        }

        String filename = audio.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = contentType.contains("mp4") || contentType.contains("m4a")
                    ? "question.m4a"
                    : "question.webm";
        }

        final MediaType uploadMediaType = mediaType;
        final String uploadFilename = filename;

        log.info("Groq STT: {} bytes, filename={}, contentType={}, model={}",
                audioBytes.length, uploadFilename, contentType, sttModel);

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return uploadFilename;
            }
        }).contentType(uploadMediaType);
        body.part("model", sttModel);
        body.part("language", "en");
        body.part("response_format", "json");
        body.part("temperature", "0");
        body.part("prompt", buildTechnicalVocabularyPrompt(jobDescription, resume));

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/openai/v1/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Object textValue = response == null ? null : response.get("text");
            String transcript = textValue == null ? "" : textValue.toString();

            log.info("Groq STT transcript: {}", transcript);
            return transcript.trim();
        } catch (WebClientResponseException e) {
            log.error("Groq STT failed: HTTP {} {}. Response body: {}",
                    e.getStatusCode().value(),
                    e.getStatusText(),
                    e.getResponseBodyAsString());
            throw e;
        }
    }

    public TranscribeResponse transcribeAndAsk(MultipartFile audio, String jobDescription, String resume,
                                                List<Map<String, String>> history) throws Exception {
        String transcript = transcribe(audio, jobDescription, resume);
        String answer = transcript.isBlank()
                ? "Could not transcribe audio. Please try again."
                : groqLLMService.answerQuestion(transcript, jobDescription, resume, history);

        return TranscribeResponse.builder()
                .transcript(transcript.isBlank() ? "(empty transcript)" : transcript)
                .answer(answer)
                .model("groq/" + sttModel)
                .build();
    }

    private String buildTechnicalVocabularyPrompt(String jobDescription, String resume) {
        // Groq documents that the Whisper prompt can guide spelling of unfamiliar words.
        // Keep this comfortably below the documented 224-token prompt limit.
        StringBuilder prompt = new StringBuilder("""
                Software engineering technical interview. Preserve exact technical names and spellings.
                Common terms include: Java, JVM, Kotlin, Spring, Spring Boot, REST, API, HTTP, JSON,
                OAuth, JWT, Kafka, RabbitMQ, Cassandra, PostgreSQL, SQL, NoSQL, Docker, Kubernetes,
                AWS, Azure, GCP, BigQuery, microservices, HashMap, HashSet, Hashtable, ArrayList,
                LinkedList, Queue, Deque, Stack, Binary Tree, Graph, Trie, Heap, recursion,
                dynamic programming, backtracking, time complexity, space complexity, Big O,
                multithreading, concurrency, synchronization, deadlock, garbage collection,
                CompletableFuture, dependency injection, SOLID, design patterns, ACID, CAP theorem,
                transactions, indexing, caching, load balancing, circuit breaker, idempotency,
                serialization, deserialization, unit testing, CI/CD.
                The audio is an interviewer asking a software engineering question. Prefer these
                technical spellings over phonetically similar everyday words.
                """);

        appendContextTerms(prompt, jobDescription, 500);
        appendContextTerms(prompt, resume, 700);
        return prompt.toString().trim();
    }

    private void appendContextTerms(StringBuilder prompt, String context, int maxChars) {
        if (context == null || context.isBlank()) return;
        String compact = context.replaceAll("\\s+", " ").trim();
        if (compact.length() > maxChars) compact = compact.substring(0, maxChars);
        prompt.append(" Context from this interview: ").append(compact);
    }
}
