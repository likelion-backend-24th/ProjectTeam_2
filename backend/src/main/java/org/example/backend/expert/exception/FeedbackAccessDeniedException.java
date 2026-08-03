package org.example.backend.expert.exception;

public class FeedbackAccessDeniedException extends RuntimeException {
    public FeedbackAccessDeniedException(String message) {
        super(message);
    }
}