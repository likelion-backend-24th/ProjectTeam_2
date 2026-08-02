package org.example.backend.study.exception;

public class StudyAlreadyJoinedException extends RuntimeException {
    public StudyAlreadyJoinedException() {
        super("이미 가입한 스터디입니다.");
    }
}