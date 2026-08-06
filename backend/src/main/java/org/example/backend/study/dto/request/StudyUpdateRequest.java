package org.example.backend.study.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StudyUpdateRequest {

    @NotBlank(message = "스터디 제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다.")
    private String description;

    @NotNull(message = "모집 인원을 입력해주세요.")
    @Positive(message = "모집 인원은 1명 이상이어야 합니다.")
    private Integer capacity;

    @NotNull(message = "모집 시작일을 입력해주세요.")
    private LocalDate recruitStart;

    @NotNull(message = "모집 마감일을 입력해주세요.")
    private LocalDate recruitEnd;

    @AssertTrue(message = "모집 마감일은 시작일보다 빠를 수 없습니다.")
    public boolean isRecruitPeriodValid() {
        if (recruitStart == null || recruitEnd == null) return true;
        return !recruitEnd.isBefore(recruitStart);
    }

}