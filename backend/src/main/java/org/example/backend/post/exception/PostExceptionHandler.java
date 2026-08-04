package org.example.backend.post.exception;


import org.example.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "org.example.backend.post")
public class PostExceptionHandler extends RuntimeException {

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(PostNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("POST_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(PostAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(PostAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("POST_ACCESS_DENIED", e.getMessage()));
    }
}
