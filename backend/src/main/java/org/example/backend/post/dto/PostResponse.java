package org.example.backend.post.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.post.entity.Post;
import org.example.backend.post.entity.PostCategory;

@Getter
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private PostCategory category;
    private String categoryLabel;
    private String authorNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .categoryLabel(post.getCategory().getLabel())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}