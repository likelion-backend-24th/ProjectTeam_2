package org.example.backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentPrepareResponse {
    @Schema(description = "결제 시도 식별자 (PortOne 결제창 호출에 사용)", example = "p2g_3f2a1b4c...")
    private String paymentId;

    @Schema(description = "PortOne 상점 ID")
    private String storeId;

    @Schema(description = "PortOne 채널 키 (일반결제용)")
    private String channelKey;

    @Schema(description = "서버가 계산한 결제 금액(원)", example = "9900")
    private Integer amount;

    @Schema(description = "결제창에 표시될 상품명", example = "prep2gether 베이직 구독")
    private String orderName;
}
