package org.example.backend.expert.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExpertErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    EXPERT_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 전문가입니다."),
    EXPERT_APPROVE_INVALID_STATUS(HttpStatus.CONFLICT, "PENDING 상태의 신청만 승인할 수 있습니다."),
    EXPERT_REJECT_INVALID_STATUS(HttpStatus.CONFLICT, "PENDING 상태의 신청만 거절할 수 있습니다."),
    EXPERT_REVOKE_INVALID_STATUS(HttpStatus.CONFLICT, "APPROVED 상태의 전문가만 자격 박탈할 수 있습니다."),
    EXPERT_REAPPLY_INVALID_STATUS(HttpStatus.CONFLICT, "거절된 신청만 재신청할 수 있습니다."),
    EXPERT_UPDATE_INVALID_STATUS(HttpStatus.CONFLICT, "PENDING 상태의 신청만 수정할 수 있습니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문의입니다."),
    SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "구독자만 이용할 수 있습니다."),
    FEEDBACK_EXPERT_NOT_APPROVED(HttpStatus.FORBIDDEN, "승인된 전문가가 아닙니다."),
    FEEDBACK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 문의의 요청자 또는 담당 전문가만 메시지를 남길 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}

/**
 * 발생할 수 있는 모든 비즈니스 예외 상황을 미리 정의
 * Point : 상태 코드 + 메시지
 *
 * HttpStatus.NOT_FOUND → 404
 * HttpStatus.CONFLICT → 409
 * HttpStatus.FORBIDDEN → 403
 */
