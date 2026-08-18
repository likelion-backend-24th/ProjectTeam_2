package org.example.backend.post.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.post.entity.Post;
import org.example.backend.post.entity.PostCategory;

@Getter
@Builder
public class PostResponse {
    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "신입 개발자 면접 후기 공유합니다")
    private String title;

    @Schema(description = "게시글 내용", example = "지난주에 본 면접 후기 남깁니다...")
    private String content;

    @Schema(description = "카테고리", example = "INTERVIEW_REVIEW")
    private PostCategory category;

    @Schema(description = "카테고리 라벨", example = "면접후기")
    private String categoryLabel;

    @Schema(description = "조회수", example = "42")
    private long viewCount;

    @Schema(description = "작성자 닉네임", example = "안양개발자")
    private String authorNickname;

    @Schema(description = "작성일시", example = "2026-08-05T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2026-08-05T10:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "이미지 URL 목록")
    private List<String> imageUrls;

    public static PostResponse from(Post post, List<String> imageUrls) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .categoryLabel(post.getCategory().getLabel())
                .viewCount(post.getViewCount())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .imageUrls(imageUrls)
                .build();
    }
}