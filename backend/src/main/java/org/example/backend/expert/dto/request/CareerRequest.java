package org.example.backend.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.expert.entity.JobField;

/**
 * 전문가 신청 시 경력 1건 입력값.
 */
@Getter
@Setter
@NoArgsConstructor
public class CareerRequest {

    @NotBlank(message = "회사명은 필수입니다.")
    private String companyName;

    @NotBlank(message = "직함은 필수입니다.")
    private String position;

    @NotNull(message = "경력 연차는 필수입니다.")
    private Integer years;

    @NotNull(message = "직무 분야는 필수입니다.")
    private JobField jobField;
}