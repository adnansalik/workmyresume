package io.github.adnansaliq.workmyresume.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SignupResponse {
    private String message = "";
    private String email = null;
    private Boolean isActive = null;
}
