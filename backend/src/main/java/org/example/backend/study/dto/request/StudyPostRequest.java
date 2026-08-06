package org.example.backend.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class StudyPostRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
}
