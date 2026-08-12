package org.example.backend.report.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 글입니다."),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고 접수 되었습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신고 내역입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}