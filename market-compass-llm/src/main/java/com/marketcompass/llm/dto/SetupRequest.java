package com.marketcompass.llm.dto;

import lombok.Data;

@Data
public class SetupRequest {
    private String jobDescription;
    /** Optional. When supplied, it becomes the saved candidate resume. */
    private String resume;
}
