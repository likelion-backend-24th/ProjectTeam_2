package org.example.backend.post.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String authorNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}