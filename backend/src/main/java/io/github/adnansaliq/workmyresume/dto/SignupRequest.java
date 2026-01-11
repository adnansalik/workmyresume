package io.github.adnansaliq.workmyresume.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class SignupRequest {

    @Email
    @NotBlank
    String email;

    @NotBlank
    @Size(min = 8, max = 64)
    String password;
}
