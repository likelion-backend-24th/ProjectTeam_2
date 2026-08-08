package org.example.backend.expert.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ExpertSignupRequest {

    @Schema(description = "경력 목록 (최소 1건 이상 필수)")
    @NotEmpty(message = "경력은 최소 1건 이상 입력해야 합니다.")
    @Valid
    private List<CareerRequest> careers;

    @Schema(description = "자격증 목록 (선택)")
    @Valid
    private List<CertificationRequest> certifications;

    // TODO: 소개글을 필수로 할지,선택으로 할지 확인 필요함.
    //  필수로 할거면 @NotBlank 추가해야 함.
    @Schema(description = "자기소개 (선택, 최대 500자)", example = "5년차 백엔드 개발자입니다.")
    @Size(max = 500, message = "소개글은 500자를 넘을 수 없습니다.")
    private String introduction;
}