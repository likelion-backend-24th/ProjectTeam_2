package org.example.backend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import org.example.backend.study.entity.StudyCategory;

import java.time.LocalDate;

@Getter
public class StudyRequest {

    @Schema(description = "스터디 제목", example = "알고리즘 스터디")
    @NotBlank(message = "스터디 제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @Schema(description = "스터디 소개", example = "매주 알고리즘 문제 3문제 풀어요.")
    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다.")
    private String description;

    @Schema(description = "모집 인원 (2명 이상)", example = "5")
    @NotNull(message = "모집 인원을 입력해주세요.")
    @Min(value = 2, message = "모집 인원은 2명 이상이어야 합니다.")
    private Integer capacity;

    @Schema(description = "모집 마감일", example = "2026-08-20")
    private LocalDate recruitEnd;

    @Schema(description = "스터디 카테고리", example = "IT_DEVELOPMENT")
    @NotNull(message = "카테고리를 선택해주세요.")
    private StudyCategory category;

    @AssertTrue(message = "모집 마감일은 오늘보다 빠를 수 없습니다.")
    public boolean isRecruitEndValid() {
        if (recruitEnd == null) return true; // null이면 상시 모집으로 간주, 검증 통과
        return !recruitEnd.isBefore(LocalDate.now());
    }

}