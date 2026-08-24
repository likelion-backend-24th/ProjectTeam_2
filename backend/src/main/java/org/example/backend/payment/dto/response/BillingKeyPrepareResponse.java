package org.example.backend.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

// 빌링키 발급 준비 응답
@Getter
@Builder
public class BillingKeyPrepareResponse {

    @Schema(description = "PortOne 상점 ID", example = "store-bfa1cc62-7...")
    private String storeId;

    @Schema(description = "빌링 채널 키", example = "channel-key-c0736ef7-...")
    private String channelKey;

    @Schema(description = "빌링키 발급 고유 ID", example = "p2g-csh-billing-a1b2c3d4")
    private String issueId; //프론트가 이값을 포트원한테 넘겨서 발급 시도
}