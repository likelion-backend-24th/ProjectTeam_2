package org.example.backend.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.payment.entity.Payment;

@Getter
@Builder
public class PaymentReadyResponse {

    @Schema(description = "PortOne 결제창 호출에 사용할 결제 고유 ID", example = "p2g-ty-3f9a1c2e-...")
    private String paymentId;

    @Schema(description = "서버가 확정한 결제 금액", example = "9900")
    private Long amount;

    @Schema(description = "통화", example = "KRW")
    private String currency;

    @Schema(description = "결제창에 표시할 주문명", example = "프리미엄 구독 1개월")
    private String orderName;

    @Schema(description = "PortOne Store ID")
    private String storeId;

    @Schema(description = "PortOne 일반결제 채널 키")
    private String channelKey;

    public static PaymentReadyResponse of(Payment payment, String storeId, String channelKey) {
        return PaymentReadyResponse.builder()
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .orderName(payment.getOrderName())
                .storeId(storeId)
                .channelKey(channelKey)
                .build();
    }
}
