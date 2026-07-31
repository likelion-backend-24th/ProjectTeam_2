package org.example.backend.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StudyUpdateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Positive
    private Integer capacity;

    @NotNull
    private LocalDate recruitStart;

    @NotNull
    private LocalDate recruitEnd;
}
