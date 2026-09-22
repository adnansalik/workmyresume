package io.github.adnansaliq.workmyresume.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeRequest {

    @Size(max = 5000)
    private String jobDescription;
}