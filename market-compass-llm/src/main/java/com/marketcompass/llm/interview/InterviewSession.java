package com.marketcompass.llm.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {
    private String jobDescription;
    private Instant startedAt;
    private String currentTopic;
    @Builder.Default
    private List<Map<String, String>> history = new ArrayList<>();
}
