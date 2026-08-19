package org.example.backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentCompleteRequest {
    @Schema(description = "완료 처리할 결제 시도 식별자", example = "p2g-kjs_3f2a1b4c-...")
    @NotBlank(message = "paymentId는 필수입니다.")
    private String paymentId;
}
