package org.example.backend.study.exception;

public class StudyAccessDeniedException extends RuntimeException {
    public StudyAccessDeniedException() {
        super("방장만 수행할 수 있는 작업입니다.");
    }
}