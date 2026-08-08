package org.example.backend.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.StudyPost;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StudyPostDetailResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "소속 스터디 ID", example = "1")
    private Long studyId;

    @Schema(description = "게시글 제목", example = "이번 주 스터디 공지")
    private String title;

    @Schema(description = "게시글 내용", example = "이번 주는 화요일에 모입니다.")
    private String content;

    @Schema(description = "작성자 유저 ID", example = "1")
    private Long authorId;

    @Schema(description = "작성자 닉네임", example = "안양개발자")
    private String authorNickname;

    @Schema(description = "작성일시", example = "2026-08-05T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2026-08-05T10:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "게시글에 달린 댓글 목록")
    private List<StudyPostCommentResponse> comments;

    public static StudyPostDetailResponse from(StudyPost post, List<StudyPostCommentResponse> comments) {
        return StudyPostDetailResponse.builder()
                .id(post.getId())
                .studyId(post.getStudy().getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getUser().getId())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(comments)
                .build();
    }
}
