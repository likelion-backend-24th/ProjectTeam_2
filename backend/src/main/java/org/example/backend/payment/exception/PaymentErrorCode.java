package org.example.backend.payment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    USER_INACTIVE(HttpStatus.CONFLICT, "탈퇴했거나 이용이 제한된 회원은 결제할 수 없습니다."),
    SUBSCRIPTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 구독 중입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 준비 내역이 없습니다."),
    PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 준비한 결제 건이 아닙니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 결제입니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.CONFLICT, "결제 검증에 실패했습니다."),
    PORTONE_API_ERROR(HttpStatus.BAD_GATEWAY, "결제사(PortOne) 통신 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
