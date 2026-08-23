package org.example.backend.subscription.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionResumeRequest {
    @Schema(description = "PortOne에서 새로 발급받은 빌링키 (resume/prepare로 받은 값으로 발급)", example = "billing-key-...")
    @NotBlank(message = "billingKey는 필수입니다.")
    private String billingKey;
}
