package org.example.backend.expert.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ExpertRejectRequest {

    @Schema(description = "거절 또는 자격 박탈 사유 (선택)", example = "아쉽게도 제출한 경력 정보가 확인되지 않습니다.")
    @Size(max = 255, message = "거절 사유는 255자를 넘을 수 없습니다.")
    private String reason;
}