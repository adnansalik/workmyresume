package io.github.adnansaliq.workmyresume.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SigninRequest {
    @Email
    @NotBlank
    public String email;

    @NotBlank
    public String password;
}
