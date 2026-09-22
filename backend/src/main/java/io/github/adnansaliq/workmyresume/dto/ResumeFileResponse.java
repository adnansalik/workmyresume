package io.github.adnansaliq.workmyresume.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ResumeFileResponse {
    private Long id;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private OffsetDateTime uploadedAt;
}