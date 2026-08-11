package com.marketcompass.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranscribeResponse {
    private String transcript;
    private String answer;
    private String model;
}
