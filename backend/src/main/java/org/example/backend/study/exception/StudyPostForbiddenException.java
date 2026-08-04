package org.example.backend.study.exception;

public class StudyPostForbiddenException extends RuntimeException {
    public StudyPostForbiddenException() {
        super("게시글에 대한 권한이 없습니다.");
    }
}