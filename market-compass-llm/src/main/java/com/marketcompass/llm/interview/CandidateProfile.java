package com.marketcompass.llm.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persistent candidate context. The resume is reusable across interview sessions;
 * the job description remains session-specific.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {
    private String resume;
    private Instant updatedAt;
}
