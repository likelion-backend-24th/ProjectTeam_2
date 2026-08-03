package org.example.backend.expert.exception;

public class InvalidExpertStatusException extends RuntimeException {
    public InvalidExpertStatusException(String message) {
        super(message);
    }
}