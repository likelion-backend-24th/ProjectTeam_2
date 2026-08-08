package org.example.backend.expert.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.expert.entity.JobField;


@Getter
@Setter
@NoArgsConstructor
public class CareerRequest {

    @Schema(description = "회사명", example = "카카오")
    @NotBlank(message = "회사명은 필수입니다.")
    private String companyName;

    @Schema(description = "직함/직책", example = "백엔드 개발자")
    @NotBlank(message = "직함은 필수입니다.")
    private String position;

    @Schema(description = "경력 연차", example = "3")
    @NotNull(message = "경력 연차는 필수입니다.")
    private Integer years;

    @Schema(description = "직무 분야", example = "IT_DEVELOPMENT")
    @NotNull(message = "직무 분야는 필수입니다.")
    private JobField jobField;
}