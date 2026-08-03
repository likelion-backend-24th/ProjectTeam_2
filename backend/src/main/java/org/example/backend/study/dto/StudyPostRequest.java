package org.example.backend.study.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
public class StudyPostRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
}
