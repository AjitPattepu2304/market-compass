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
        log.info("Groq STT: {} bytes", audio.getSize());

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", new ByteArrayResource(audio.getBytes()) {
            @Override public String getFilename() { return "audio.webm"; }
        }, MediaType.parseMediaType("audio/webm"));
        body.part("model", sttModel);
        body.part("response_format", "json");

        Map<?, ?> response = webClient.post()
                .uri("/openai/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String transcript = response != null ? (String) response.get("text") : "";
        log.info("Transcript: {}", transcript);
        return transcript != null ? transcript.trim() : "";
    }

    public TranscribeResponse transcribeAndAsk(MultipartFile audio, String jobDescription, String resume) throws Exception {
        String transcript = transcribe(audio);
        String answer = transcript.isBlank()
                ? "Could not transcribe audio. Please try speaking more clearly."
                : groqLLMService.answerQuestion(transcript, jobDescription, resume);
        return TranscribeResponse.builder()
                .transcript(transcript.isBlank() ? "(empty transcript)" : transcript)
                .answer(answer)
                .model("groq/" + sttModel)
                .build();
    }
}
