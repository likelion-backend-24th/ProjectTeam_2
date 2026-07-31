package org.example.backend.user.exception;


// 요청한 사용자르 찾을 수 없을 떄 던지는 예외
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}