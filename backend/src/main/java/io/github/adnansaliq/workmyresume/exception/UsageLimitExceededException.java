package io.github.adnansaliq.workmyresume.exception;

public class UsageLimitExceededException extends RuntimeException {

    public UsageLimitExceededException(String message) {
        super(message);
    }
}