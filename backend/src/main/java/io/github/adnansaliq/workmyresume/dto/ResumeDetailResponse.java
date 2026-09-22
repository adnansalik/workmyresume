package io.github.adnansaliq.workmyresume.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ResumeDetailResponse {
    private Long id;
    private String title;
    private OffsetDateTime createdAt;
    private List<ResumeVersionResponse> versions = new ArrayList<>();
}