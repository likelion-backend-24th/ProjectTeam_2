package org.example.backend.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.payment.entity.BillingKey;

import java.time.LocalDateTime;

/**
 * 등록된 카드 정보. 카드번호·카드사는 우리 서버가 보관하지 않으므로 등록 여부와 등록 시각만 내려준다.
 */
@Getter
@Builder
public class BillingKeyResponse {

    @Schema(description = "정기결제용 카드가 등록되어 있는지", example = "true")
    private boolean registered;

    @Schema(description = "카드 등록 일시. 등록된 카드가 없으면 null", example = "2026-08-24T02:07:18")
    private LocalDateTime issuedAt;

    @Schema(description = "카드전표인자명. 카드사·채널에 따라 내려오지 않을 수 있어 null 가능", example = "신한카드")
    private String cardName;

    @Schema(description = "마스킹된 카드번호. 마찬가지로 null 가능", example = "433012******1234")
    private String cardNumberMasked;

    public static BillingKeyResponse of(BillingKey billingKey) {
        return BillingKeyResponse.builder()
                .registered(true)
                .issuedAt(billingKey.getIssuedAt())
                .cardName(billingKey.getCardName())
                .cardNumberMasked(billingKey.getCardNumberMasked())
                .build();
    }

    public static BillingKeyResponse empty() {
        return BillingKeyResponse.builder().registered(false).build();
    }
}
