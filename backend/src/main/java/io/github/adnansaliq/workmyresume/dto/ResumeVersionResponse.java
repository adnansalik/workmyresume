package io.github.adnansaliq.workmyresume.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ResumeVersionResponse {
    private Long id;
    private Long resumeId;
    private Integer versionNumber;
    private String title;
    private String rawText;
    private OffsetDateTime createdAt;
    private List<ResumeFileResponse> files = new ArrayList<>();
}