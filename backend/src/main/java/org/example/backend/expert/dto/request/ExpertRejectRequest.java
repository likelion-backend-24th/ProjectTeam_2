package org.example.backend.expert.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 1. 전문가 거절(PATCH /api/admin/experts/{id}/reject)
 * 2. 자격 박탈(DELETE /api/admin/experts/{id})
 */

@Getter
@Setter
@NoArgsConstructor
public class ExpertRejectRequest {

    @Size(max = 255, message = "reason은 255자를 넘을 수 없습니다.")
    private String reason;
}