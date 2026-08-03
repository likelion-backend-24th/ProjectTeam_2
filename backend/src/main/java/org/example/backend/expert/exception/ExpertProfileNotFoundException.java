package org.example.backend.expert.exception;

public class ExpertProfileNotFoundException extends RuntimeException {
    public ExpertProfileNotFoundException(String message) {
        super(message);
    }
}