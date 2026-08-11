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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@Profile("groq")
@Slf4j
@RequiredArgsConstructor
public class GroqSpeechService {

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.stt-model:whisper-large-v3-turbo}")
    private String sttModel;

    private final GroqLLMService groqLLMService;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.groq.com")
            .build();

    public String transcribe(MultipartFile audio) throws Exception {
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
        String transcript = transcribe(audio);
        String answer = transcript.isBlank()
                ? "Could not transcribe audio. Please try again."
                : groqLLMService.answerQuestion(transcript, jobDescription, resume, history);

        return TranscribeResponse.builder()
                .transcript(transcript.isBlank() ? "(empty transcript)" : transcript)
                .answer(answer)
                .model("groq/" + sttModel)
                .build();
    }
}
