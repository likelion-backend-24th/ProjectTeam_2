package org.example.backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentPrepareResponse {
    @Schema(description = "빌링키 발급 시도 식별자 (프론트 requestIssueBillingKey 호출에 사용)", example = "p2g-kjs_3f2a1b4c...")
    private String issueId;

    @Schema(description = "PortOne 상점 ID")
    private String storeId;

    @Schema(description = "PortOne 채널 키 (빌링키 발급용)")
    private String channelKey;

    @Schema(description = "서버가 계산한 결제 금액(원)", example = "9900")
    private Integer amount;

    @Schema(description = "결제창에 표시될 상품명", example = "prep2gether 베이직 구독")
    private String orderName;
}
