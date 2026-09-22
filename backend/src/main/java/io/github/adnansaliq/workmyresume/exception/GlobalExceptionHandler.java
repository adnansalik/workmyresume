package io.github.adnansaliq.workmyresume.exception;

import io.github.adnansaliq.workmyresume.dto.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;


@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex){
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(ex.getMessage());
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(Exception ex){
        // Respect the status/message of ResponseStatusException (e.g. thrown by controllers)
        // instead of masking it inside the generic 500 handler.
        if (ex instanceof ResponseStatusException responseStatusException) {
            ExceptionResponse response = new ExceptionResponse();
            response.setMessage(responseStatusException.getReason());
            response.setTimestamp(OffsetDateTime.now());
            return new ResponseEntity<>(response, responseStatusException.getStatusCode());
        }

        ExceptionResponse response = new ExceptionResponse();
        response.setMessage("An unexpected error occurred");
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){
        System.out.println("Bad Request"+ex.getMessage());
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage("Bad request");
        response.setTimestamp(OffsetDateTime.now());
        Map<String,String> errors = new HashMap<>();
        for(FieldError error: ex.getBindingResult().getFieldErrors()){
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        response.setErrors(errors);

        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex){
        ExceptionResponse response = new ExceptionResponse();
        response.setTimestamp(OffsetDateTime.now());

        if (ex.getRootCause() != null) {
            String rootMsg = ex.getRootCause().getMessage();
            if (rootMsg != null && rootMsg.contains("Required request body")) {
                response.setMessage("Required request body is missing");
            }
            response.setMessage("Invalid request body");
        }
        response.setMessage("Malformed JSON request");

        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleBadCredentials() {

        ExceptionResponse response = new ExceptionResponse();
        response.setMessage("Invalid credentials");
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response,HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ExceptionResponse> handleDisabledAccount() {

        ExceptionResponse response = new ExceptionResponse();
        response.setMessage("Account is disabled");
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAccessDenied(AccessDeniedException ex) {
        // @PreAuthorize failures surface as AuthorizationDeniedException (extends
        // AccessDeniedException) inside the servlet; they must stay 403, not be
        // swallowed by the generic 500 handler.
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage("Access denied");
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UsageLimitExceededException.class)
    public ResponseEntity<ExceptionResponse> handleUsageLimit(UsageLimitExceededException ex){
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(ex.getMessage());
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleResourceNotFound(ResourceNotFoundException ex){
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(ex.getMessage());
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleUsernameNotFound(UsernameNotFoundException ex){
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage("User not found");
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ExceptionResponse> handleUnsupportedFileType(UnsupportedFileTypeException ex){
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(ex.getMessage());
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ExceptionResponse> handleBadRequest(BadRequestException ex){
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(ex.getMessage());
        response.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
