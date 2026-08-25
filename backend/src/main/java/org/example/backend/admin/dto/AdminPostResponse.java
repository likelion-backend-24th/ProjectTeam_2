package org.example.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.post.entity.Post;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPostResponse {
    @Schema(description = "게시글 ID", example = "1")
    private Long id;
    @Schema(description = "게시글 제목", example = "신입 개발자 면접 후기 공유합니다")
    private String title;
    @Schema(description = "카테고리", example = "INTERVIEW_REVIEW")
    private String category;
    @Schema(description = "카테고리 라벨", example = "면접후기")
    private String categoryLabel;
    @Schema(description = "작성자 유저 ID", example = "1")
    private Long authorId;
    @Schema(description = "작성자 닉네임", example = "안양개발자")
    private String authorNickname;
    @Schema(description = "조회수", example = "42")
    private long viewCount;
    @Schema(description = "작성일시", example = "2026-08-05T10:00:00")
    private LocalDateTime createdAt;

    public static AdminPostResponse from(Post post) {
        return AdminPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .category(post.getCategory().name())
                .categoryLabel(post.getCategory().getLabel())
                .authorId(post.getUser().getId())
                .authorNickname(post.getUser().getNickname())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
