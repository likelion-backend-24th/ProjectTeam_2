package org.example.backend.expert.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


/**
 * 전문가 신청(POST /api/experts/signup) 요청 바디.
 * 사용 : 신규 신청, 거절 후 재신청할 때
 */

@Getter
@Setter
@NoArgsConstructor
public class ExpertSignupRequest {

    @NotEmpty(message = "경력은 최소 1건 이상 입력해야 합니다.")
    @Valid
    private List<CareerRequest> careers;

    @Valid
    private List<CertificationRequest> certifications; // 선택 사항

    @Size(max = 500, message = "소개글은 500자를 넘을 수 없습니다.")
    private String introduction;
}