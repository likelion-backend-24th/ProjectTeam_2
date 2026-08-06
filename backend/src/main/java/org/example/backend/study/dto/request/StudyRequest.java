package org.example.backend.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StudyRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private Integer capacity;
    @NotNull
    private LocalDate recruitStart;
    @NotNull
    private LocalDate recruitEnd;
}
