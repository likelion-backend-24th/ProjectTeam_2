package org.example.backend.comment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "댓글에 대한 권한이 없습니다."),
    COMMENT_POST_MISMATCH(HttpStatus.NOT_FOUND, "해당 게시글에 속한 댓글이 아닙니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
