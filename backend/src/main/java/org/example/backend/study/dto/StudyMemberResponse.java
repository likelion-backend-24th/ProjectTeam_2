package org.example.backend.study.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudyMemberResponse {
    private Long memberId;
    private Long userId;
    private String nickname;
    private LocalDateTime joinedAt;
}