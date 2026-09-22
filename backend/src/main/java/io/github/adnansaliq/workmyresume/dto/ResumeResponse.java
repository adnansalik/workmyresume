package io.github.adnansaliq.workmyresume.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ResumeResponse {
    private Long id;
    private String title;
    private OffsetDateTime createdAt;
    private Integer versionCount;
}