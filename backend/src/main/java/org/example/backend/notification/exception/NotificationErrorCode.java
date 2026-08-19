package org.example.backend.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "알림에 대한 접근이 거부되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}