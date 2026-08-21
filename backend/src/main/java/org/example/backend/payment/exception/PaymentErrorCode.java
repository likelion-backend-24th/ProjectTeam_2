package org.example.backend.payment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 결제입니다."),
    PAYMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 결제만 처리할 수 있습니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.CONFLICT, "결제 검증에 실패했습니다."),
    WEBHOOK_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "웹훅 서명 검증에 실패했습니다."),
    BILLING_KEY_VERIFICATION_FAILED(HttpStatus.CONFLICT, "빌링키 검증에 실패했습니다."),
    BILLING_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 결제 수단이 없습니다."),
    BILLING_KEY_DELETE_FAILED(HttpStatus.CONFLICT, "결제 수단 삭제에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
