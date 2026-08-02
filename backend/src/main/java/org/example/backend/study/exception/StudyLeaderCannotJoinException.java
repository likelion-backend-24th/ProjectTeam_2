package org.example.backend.study.exception;

public class StudyLeaderCannotJoinException extends RuntimeException {
    public StudyLeaderCannotJoinException() {
        super("방장은 본인 스터디에 가입할 수 없습니다.");
    }
}