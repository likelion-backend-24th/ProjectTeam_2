package org.example.backend.expert.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ExpertRejectRequest {

    @Size(max = 255, message = "reason은 255자를 넘을 수 없습니다.")
    private String reason;
}