package org.example.backend.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PaymentCompleteRequest(

        @Schema(description = "결제 준비 단계에서 발급받은 결제 ID", example = "p2g-ty-3f9a1c2e-...")
        @NotBlank(message = "paymentId는 필수입니다.")
        String paymentId
) {
}
