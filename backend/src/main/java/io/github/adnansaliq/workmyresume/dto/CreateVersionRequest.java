package io.github.adnansaliq.workmyresume.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVersionRequest {

    @Size(max = 255)
    private String title;
}