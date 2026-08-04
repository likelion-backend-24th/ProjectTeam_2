package org.example.backend.post.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.post.entity.PostCategory;

@Getter
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private PostCategory category;
    private String authorNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}