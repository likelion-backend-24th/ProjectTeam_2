package org.example.backend.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 빌링키 발급 검증 요청, 프론트가 PortOne 카드 등록 끝낸 후 보내는 값
@Getter
@NoArgsConstructor
public class BillingKeyIssueRequest {

    @Schema(description = "발급 고유 ID (prepare 단계에서 받은 값)", example = "p2g-csh-billing-a1b2c3d4")
    @NotBlank
    private String issueId;

    @Schema(description = "PortOne이 발급한 빌링키", example = "billing-key-xxxx")
    @NotBlank
    private String billingKey;
}