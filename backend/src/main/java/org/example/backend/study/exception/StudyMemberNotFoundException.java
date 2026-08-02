package org.example.backend.study.exception;

public class StudyMemberNotFoundException extends RuntimeException {
    public StudyMemberNotFoundException() {
        super("가입한 멤버만 이용할 수 있습니다.");
    }
}