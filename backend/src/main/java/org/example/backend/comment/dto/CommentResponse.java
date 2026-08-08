package org.example.backend.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.comment.entity.Comment;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {
    @Schema(description = "댓글 ID", example = "1")
    private Long id;

    @Schema(description = "댓글 내용", example = "좋은 정보입니다!")
    private String content;

    @Schema(description = "작성자 닉네임", example = "안양개발자")
    private String authorNickname;

    @Schema(description = "작성일시", example = "2026-08-05T10:00:00")
    private LocalDateTime createdAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorNickname(comment.getUser().getNickname())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}