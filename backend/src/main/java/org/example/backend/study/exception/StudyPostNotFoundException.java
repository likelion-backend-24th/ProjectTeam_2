package org.example.backend.study.exception;

public class StudyPostNotFoundException extends RuntimeException {
    public StudyPostNotFoundException() {
        super("스터디 멤버가 아닙니다.");
    }
}
