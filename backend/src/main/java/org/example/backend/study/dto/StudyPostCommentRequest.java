package org.example.backend.study.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class StudyPostCommentRequest {
    @NotBlank
    private String content;
}
