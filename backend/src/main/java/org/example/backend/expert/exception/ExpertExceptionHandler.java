package org.example.backend.expert.exception;

import org.example.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "org.example.backend.expert")
public class ExpertExceptionHandler {

    @ExceptionHandler(ExpertProfileNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ExpertProfileNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("EXPERT_PROFILE_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(InvalidExpertStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatus(InvalidExpertStatusException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("EXPERT_INVALID_STATUS", e.getMessage()));
    }
}