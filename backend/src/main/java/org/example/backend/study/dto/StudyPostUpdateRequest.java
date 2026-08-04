package org.example.backend.study.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class StudyPostUpdateRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
}
