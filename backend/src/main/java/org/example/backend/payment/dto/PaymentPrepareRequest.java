package org.example.backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.payment.entity.SubscriptionPlanType;

@Getter
@Setter
@NoArgsConstructor
public class PaymentPrepareRequest {
    @Schema(description = "구독할 플랜 종류", example = "BASIC")
    @NotNull(message = "planType은 필수입니다.")
    private SubscriptionPlanType planType;
}
