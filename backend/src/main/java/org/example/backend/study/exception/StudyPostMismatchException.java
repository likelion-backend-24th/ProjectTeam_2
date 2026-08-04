package org.example.backend.study.exception;

public class StudyPostMismatchException extends RuntimeException {
    public StudyPostMismatchException() {
        super("해당 스터디에 속한 게시글이 아닙니다.");
    }
}
