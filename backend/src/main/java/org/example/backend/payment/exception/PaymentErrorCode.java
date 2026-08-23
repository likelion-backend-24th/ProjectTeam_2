package org.example.backend.payment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    BILLING_KEY_ISSUANCE_INTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 발급 요청입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 요청이 아닙니다."),
    BILLING_KEY_VERIFICATION_FAILED(HttpStatus.CONFLICT, "빌링키 검증에 실패했습니다."),
    BILLING_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 빌링키가 없습니다."),
    ALREADY_SUBSCRIBED(HttpStatus.CONFLICT, "이미 구독 중입니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.CONFLICT, "결제 검증에 실패했습니다."),
    SUBSCRIPTION_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "예약된 정기결제가 없습니다.");


    private final HttpStatus httpStatus;
    private final String message;
}