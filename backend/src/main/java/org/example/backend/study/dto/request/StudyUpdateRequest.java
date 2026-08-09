package org.example.backend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import org.example.backend.study.entity.StudyCategory;

import java.time.LocalDate;

@Getter
public class StudyUpdateRequest {

    @Schema(description = "수정할 스터디 제목", example = "자격증 스터디 (수정)")
    @NotBlank(message = "스터디 제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @Schema(description = "수정할 스터디 소개", example = "각자 자격증 공부를 해봐요.")
    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다.")
    private String description;

    @Schema(description = "수정할 모집 인원 (1명 이상)", example = "8")
    @NotNull(message = "모집 인원을 입력해주세요.")
    @Positive(message = "모집 인원은 1명 이상이어야 합니다.")
    private Integer capacity;

    @Schema(description = "수정할 모집 마감일", example = "2026-08-25")
    private LocalDate recruitEnd;

    @Schema(description = "수정할 스터디 카테고리", example = "CERTIFICATE")
    @NotNull(message = "카테고리를 선택해주세요.")
    private StudyCategory category;

    @AssertTrue(message = "모집 마감일은 오늘보다 빠를 수 없습니다.")
    public boolean isRecruitEndValid() {
        if (recruitEnd == null) return true; // null이면 상시 모집으로 간주, 검증 통과
        return !recruitEnd.isBefore(LocalDate.now());
    }

}