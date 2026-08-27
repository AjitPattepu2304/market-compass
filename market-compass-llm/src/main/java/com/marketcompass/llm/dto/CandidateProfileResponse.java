package com.marketcompass.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CandidateProfileResponse {
    private boolean available;
    private Instant updatedAt;
}
