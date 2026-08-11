package com.marketcompass.llm.service;

import com.marketcompass.llm.dto.TranscribeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Profile("!groq")
@Slf4j
@RequiredArgsConstructor
public class SpeechService {

    @Value("${whisper.binary:/opt/homebrew/bin/whisper-cli}")
    private String whisperBinary;

    @Value("${whisper.model:/opt/homebrew/share/whisper-cpp/ggml-base.en.bin}")
    private String whisperModel;

    private final LLMService llmService;

    public String transcribe(MultipartFile audio) throws Exception {
        Path tmpWebm = Files.createTempFile("audio_", ".webm");
        Path tmpWav  = Files.createTempFile("audio_", ".wav");
        Path txtOut  = Path.of(tmpWav + ".txt");

        audio.transferTo(tmpWebm);

        try {
            new ProcessBuilder("/opt/homebrew/bin/ffmpeg", "-y", "-i", tmpWebm.toString(),
                    "-ar", "16000", "-ac", "1", "-f", "wav", tmpWav.toString())
                    .redirectErrorStream(true).start().waitFor();

            new ProcessBuilder(whisperBinary, "-m", whisperModel, "-f", tmpWav.toString(), "--output-txt")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start().waitFor();

            if (!Files.exists(txtOut)) {
                log.error("Whisper txt output not found: {}", txtOut);
                return "";
            }

            String transcript = Files.readString(txtOut).trim();
            log.info("Transcript: {}", transcript);
            return transcript;

        } finally {
            Files.deleteIfExists(tmpWebm);
            Files.deleteIfExists(tmpWav);
            Files.deleteIfExists(txtOut);
        }
    }

    public TranscribeResponse transcribeAndAsk(MultipartFile audio, String jobDescription, String resume) throws Exception {
        String transcript = transcribe(audio);
        String answer = transcript.isBlank()
                ? "Could not transcribe audio. Please try speaking more clearly."
                : llmService.answerQuestion(transcript, jobDescription, resume);
        return TranscribeResponse.builder()
                .transcript(transcript.isBlank() ? "(empty transcript)" : transcript)
                .answer(answer)
                .model("ollama/llama3.2:3b")
                .build();
    }
}
