package org.example.backend.study.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.StudyPost;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudyPostResponse {
    private Long id;
    private Long studyId;
    private String title;
    private String content;
    private Long authorId;
    private String authorNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StudyPostResponse from(StudyPost studyPost) {
        return StudyPostResponse.builder()
                .id(studyPost.getId())
                .studyId(studyPost.getStudy().getId())
                .title(studyPost.getTitle())
                .content(studyPost.getContent())
                .authorId(studyPost.getUser().getId())
                .authorNickname(studyPost.getUser().getNickname())
                .createdAt(studyPost.getCreatedAt())
                .updatedAt(studyPost.getUpdatedAt())
                .build();
    }
}
