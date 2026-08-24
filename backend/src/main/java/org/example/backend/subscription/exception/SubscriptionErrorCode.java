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
    USER_INACTIVE(HttpStatus.CONFLICT, "탈퇴했거나 이용이 제한된 회원은 구독할 수 없습니다."),
    GRACE_PERIOD_ENDED(HttpStatus.CONFLICT, "유예기간이 이미 종료되었습니다."),
    RETRY_TOO_SOON(HttpStatus.CONFLICT, "방금 재시도했어요. 잠시 후 다시 시도해주세요."),
    NO_ACTIVE_BILLING_KEY(HttpStatus.CONFLICT, "자동갱신 중인 결제수단이 없어요. 해지 예약 취소를 먼저 진행해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}