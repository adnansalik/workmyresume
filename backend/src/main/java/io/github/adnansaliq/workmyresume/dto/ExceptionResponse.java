package io.github.adnansaliq.workmyresume.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;

@Setter
@Getter
public class ExceptionResponse {

    private String message;
    private OffsetDateTime timestamp;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String,String> errors;
}
