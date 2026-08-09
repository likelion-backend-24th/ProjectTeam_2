package org.example.backend.study.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StudyErrorCode implements ErrorCode {

    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스터디입니다."),
    STUDY_MEMBER_NOT_FOUND(HttpStatus.FORBIDDEN, "스터디 멤버가 아닙니다."),
    STUDY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "방장만 수행할 수 있는 작업입니다."),
    STUDY_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 가입한 스터디입니다."),
    STUDY_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "모집 정원이 초과되었습니다."),
    STUDY_RECRUIT_CLOSED(HttpStatus.CONFLICT, "마감일이 지난 스터디입니다."),
    STUDY_CAPACITY_BELOW_CURRENT_MEMBERS(HttpStatus.CONFLICT, "현재 인원보다 적은 인원으로 변경할 수 없습니다."),
    STUDY_JOIN_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "비구독자는 최대 2개의 스터디까지 개설/참여할 수 있습니다."),
    STUDY_LEADER_CANNOT_JOIN(HttpStatus.CONFLICT, "방장은 본인 스터디에 가입할 수 없습니다."),
    STUDY_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    STUDY_POST_MISMATCH(HttpStatus.NOT_FOUND, "해당 스터디에 속한 게시글이 아닙니다."),
    STUDY_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "게시글에 대한 권한이 없습니다."),
    STUDY_POST_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    STUDY_POST_COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "댓글에 대한 권한이 없습니다."),
    STUDY_LEADER_CANNOT_BE_REMOVED(HttpStatus.FORBIDDEN, "방장은 강퇴할 수 없습니다."),
    STUDY_LEADER_DELEGATE_SELF(HttpStatus.CONFLICT, "본인에게는 방장을 위임할 수 없습니다."),
    STUDY_LEADER_MUST_DELEGATE(HttpStatus.CONFLICT, "방장은 위임 후에 탈퇴할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
