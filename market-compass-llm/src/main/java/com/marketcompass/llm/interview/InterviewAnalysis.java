package com.marketcompass.llm.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnalysis {
    private QuestionType questionType;
    private String question;
    private String topic;
    @Builder.Default
    private List<String> evidence = new ArrayList<>();
    @Builder.Default
    private List<String> likelyFollowUps = new ArrayList<>();
}
