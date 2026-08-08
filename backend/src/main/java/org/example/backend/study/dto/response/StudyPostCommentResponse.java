package org.example.backend.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.StudyPostComment;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudyPostCommentResponse {
    @Schema(description = "게시글 댓글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 댓글 내용", example = "확인했습니다!")
    private String content;

    @Schema(description = "작성자 유저 ID", example = "3")
    private Long authorId;

    @Schema(description = "작성자 닉네임", example = "안양개발자")
    private String authorNickname;

    @Schema(description = "작성일시", example = "2026-08-05T11:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2026-08-05T11:00:00")
    private LocalDateTime updatedAt;

    public static StudyPostCommentResponse from(StudyPostComment comment){
        return StudyPostCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getUser().getId())
                .authorNickname(comment.getUser().getNickname())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
