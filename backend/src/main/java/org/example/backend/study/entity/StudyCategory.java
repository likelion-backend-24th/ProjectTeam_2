package org.example.backend.study.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyCategory {

    IT_DEVELOPMENT("IT개발"),
    LANGUAGE("언어"),
    CERTIFICATE("자격증"),
    JOB_PREP("취업준비"),
    ETC("기타");

    private final String label;
}
