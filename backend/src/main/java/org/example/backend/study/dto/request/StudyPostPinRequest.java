package org.example.backend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StudyPostPinRequest {
    @Schema(description = "고정 여부", example = "true")
    @NotNull
    private Boolean pinned;
}