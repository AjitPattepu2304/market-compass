package com.marketcompass.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TranscribeResponse {
    private String transcript;
    private String answer;
    private String model;
    private List<String> likelyFollowUps;
    private String questionType;
    private String topic;
}
