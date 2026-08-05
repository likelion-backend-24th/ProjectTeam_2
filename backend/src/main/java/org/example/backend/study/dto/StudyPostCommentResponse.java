package org.example.backend.study.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.StudyPostComment;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudyPostCommentResponse {
    private Long id;
    private String content;
    private Long authorId;
    private String authorNickname;
    private LocalDateTime createdAt;
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
