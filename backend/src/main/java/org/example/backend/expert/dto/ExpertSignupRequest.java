package org.example.backend.expert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 전문가 신청(POST /api/experts/signup) 요청 바디.
 * 사용 : 신규 신청, 거절 후 재신청할 때
 */

@Getter
@Setter
@NoArgsConstructor
public class ExpertSignupRequest {

    @NotBlank(message = "career는 필수입니다.")
    @Size(max = 2000, message = "career는 2000자를 넘을 수 없습니다.")
    private String career;

    @Size(max = 2000, message = "certification은 2000자를 넘을 수 없습니다.")
    private String certification;
}