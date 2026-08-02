package org.example.backend.study.exception;

public class StudyJoinLimitExceededException extends RuntimeException {
    public StudyJoinLimitExceededException() {
        super("비구독자는 최대 2개의 스터디까지 개설/참여할 수 있습니다.");
    }
}