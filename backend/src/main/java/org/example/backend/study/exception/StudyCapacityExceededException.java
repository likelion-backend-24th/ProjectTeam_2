package org.example.backend.study.exception;

public class StudyCapacityExceededException extends RuntimeException {
    public StudyCapacityExceededException() {
        super("모집 정원이 초과되었습니다.");
    }
}