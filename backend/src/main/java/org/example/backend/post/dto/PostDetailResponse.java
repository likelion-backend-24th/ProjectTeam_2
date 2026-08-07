package org.example.backend.post.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.comment.dto.CommentResponse;
import org.example.backend.comment.entity.Comment;
import org.example.backend.post.entity.Post;
import org.example.backend.post.entity.PostCategory;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PostDetailResponse {
    private Long id;
    private String title;
    private String content;
    private PostCategory category;
    private String categoryLabel;
    private long viewCount;
    private String authorNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> comments;
    private int totalCommentPages;
    private long totalComments;

    public static PostDetailResponse from(Post post, Page<Comment> commentsPage) {
        List<CommentResponse> commentResponses = commentsPage.getContent().stream()
                .map(CommentResponse::from)
                .toList();

        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .categoryLabel(post.getCategory().getLabel())
                .viewCount(post.getViewCount())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(commentResponses)
                .totalCommentPages(commentsPage.getTotalPages())
                .totalComments(commentsPage.getTotalElements())
                .build();
    }
}