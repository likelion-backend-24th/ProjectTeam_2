package org.example.backend.subscription.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    SUBSCRIPTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 구독 중입니다."),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 내역이 없습니다."),
    USER_INACTIVE(HttpStatus.CONFLICT, "탈퇴했거나 이용이 제한된 회원은 구독할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}