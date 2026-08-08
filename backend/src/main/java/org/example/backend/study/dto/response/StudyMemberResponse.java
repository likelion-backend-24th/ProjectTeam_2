package org.example.backend.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.StudyMember;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudyMemberResponse {
    @Schema(description = "스터디 멤버 ID", example = "1")
    private Long memberId;

    @Schema(description = "멤버의 유저 ID", example = "3")
    private Long userId;

    @Schema(description = "멤버 닉네임", example = "안양개발자")
    private String nickname;

    @Schema(description = "스터디 가입일시", example = "2026-08-05T10:00:00")
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