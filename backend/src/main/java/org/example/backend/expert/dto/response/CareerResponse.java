package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Career;
import org.example.backend.expert.entity.JobField;

// 경력을 통째로 보여줄 때 사용할 DTO
@Getter
@Builder
public class CareerResponse {
    @Schema(description = "경력 ID", example = "1")
    private Long id;

    @Schema(description = "회사명", example = "카카오")
    private String companyName;

    @Schema(description = "직함/직책", example = "백엔드 개발자")
    private String position;

    @Schema(description = "경력 연차", example = "3")
    private Integer years;

    @Schema(description = "직무 분야", example = "IT_DEVELOPMENT")
    private JobField jobField;

    public static CareerResponse from(Career career) {
        return CareerResponse.builder()
                .id(career.getId())
                .companyName(career.getCompanyName())
                .position(career.getPosition())
                .years(career.getYears())
                .jobField(career.getJobField())
                .build();
    }
}