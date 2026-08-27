package com.marketcompass.llm.controller;

import com.marketcompass.llm.dto.CandidateProfileResponse;
import com.marketcompass.llm.interview.CandidateProfile;
import com.marketcompass.llm.interview.CandidateProfileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview/profile")
@RequiredArgsConstructor
public class CandidateProfileController {
    private final CandidateProfileStore profileStore;

    @GetMapping
    public ResponseEntity<CandidateProfileResponse> getProfile() {
        return ResponseEntity.ok(profileStore.load()
                .map(this::response)
                .orElseGet(() -> CandidateProfileResponse.builder().available(false).build()));
    }

    @PutMapping("/resume")
    public ResponseEntity<CandidateProfileResponse> saveResume(@RequestBody ResumeRequest request) {
        CandidateProfile profile = profileStore.saveResume(request.resume());
        return ResponseEntity.ok(response(profile));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearProfile() {
        profileStore.clear();
        return ResponseEntity.noContent().build();
    }

    private CandidateProfileResponse response(CandidateProfile profile) {
        return CandidateProfileResponse.builder()
                .available(profile.getResume() != null && !profile.getResume().isBlank())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    public record ResumeRequest(String resume) {}
}
