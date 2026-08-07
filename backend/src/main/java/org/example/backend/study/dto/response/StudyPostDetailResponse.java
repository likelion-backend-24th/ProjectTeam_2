package org.example.backend.study.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.StudyPost;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StudyPostDetailResponse {
    private Long id;
    private Long studyId;
    private String title;
    private String content;
    private Long authorId;
    private String authorNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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
