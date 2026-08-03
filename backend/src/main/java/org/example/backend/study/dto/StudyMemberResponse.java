package org.example.backend.study.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.StudyMember;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudyMemberResponse {
    private Long memberId;
    private Long userId;
    private String nickname;
    private LocalDateTime joinedAt;

    public static StudyMemberResponse from(StudyMember member) {
        return StudyMemberResponse.builder()
                .memberId(member.getId())
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}