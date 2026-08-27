package com.marketcompass.llm.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Small local persistent store for the active candidate profile.
 * This intentionally keeps resume data outside HttpSession so a new interview
 * can reuse it without another upload. A database-backed implementation can
 * replace this store later without changing the interview APIs.
 */
@Service
@Slf4j
public class CandidateProfileStore {
    private final ObjectMapper objectMapper;
    private final Path profilePath;

    public CandidateProfileStore(
            ObjectMapper objectMapper,
            @Value("${interview.candidate-profile-path:${user.home}/.marketcompass/candidate-profile.json}") String path) {
        this.objectMapper = objectMapper;
        this.profilePath = Path.of(path).toAbsolutePath();
    }

    public synchronized Optional<CandidateProfile> load() {
        if (!Files.exists(profilePath)) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(profilePath.toFile(), CandidateProfile.class));
        } catch (IOException e) {
            log.warn("Unable to load candidate profile from {}", profilePath, e);
            return Optional.empty();
        }
    }

    public synchronized CandidateProfile saveResume(String resume) {
        if (resume == null || resume.isBlank()) {
            throw new IllegalArgumentException("Resume cannot be blank");
        }
        CandidateProfile profile = CandidateProfile.builder()
                .resume(resume.trim())
                .updatedAt(Instant.now())
                .build();
        try {
            Files.createDirectories(profilePath.getParent());
            Path temp = profilePath.resolveSibling(profilePath.getFileName() + ".tmp");
            objectMapper.writeValue(temp.toFile(), profile);
            Files.move(temp, profilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return profile;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save candidate profile", e);
        }
    }

    public synchronized void clear() {
        try {
            Files.deleteIfExists(profilePath);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to clear candidate profile", e);
        }
    }
}
