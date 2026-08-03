package org.example.backend.expert.exception;

import org.example.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "org.example.backend.expert")
public class FeedbackExceptionHandler {

    @ExceptionHandler(FeedbackNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(FeedbackNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("FEEDBACK_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(SubscriptionRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleSubscriptionRequired(SubscriptionRequiredException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("SUBSCRIPTION_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(FeedbackAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(FeedbackAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FEEDBACK_ACCESS_DENIED", e.getMessage()));
    }
}