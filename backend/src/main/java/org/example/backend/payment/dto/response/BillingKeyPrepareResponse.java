package org.example.backend.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillingKeyPrepareResponse {
    @Schema(description = "PortOne Store ID")
    private String storeId;

    @Schema(description = "정기결제(빌링키) 채널 키")
    private String channelKey;

    @Schema(description = "이번 카드 등록 시도를 식별하는 issueId")
    private String issueId;

    @Schema(description = "PortOne에 전달할 고객 식별자 (내부 유저 ID)")
    private String customerId;
}
