package io.github.adnansaliq.workmyresume.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ResumeAnalysisResponse {
    private Long id;
    private Long versionId;
    private String jobDescription;
    private Integer score;
    private List<String> strengths = new ArrayList<>();
    private List<String> gaps = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private OffsetDateTime createdAt;
}