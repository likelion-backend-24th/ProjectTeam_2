package org.example.backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.payment.entity.SubscriptionPlanType;

@Getter
@Setter
@NoArgsConstructor
public class PaymentCompleteRequest {
    @Schema(description = "PortOne에서 발급받은 빌링키", example = "billing-key-...")
    @NotBlank(message = "billingKey는 필수입니다.")
    private String billingKey;

    @Schema(description = "구독할 플랜 종류 (prepare 때와 동일해야 함)", example = "BASIC")
    @NotNull(message = "planType은 필수입니다.")
    private SubscriptionPlanType planType;
}
