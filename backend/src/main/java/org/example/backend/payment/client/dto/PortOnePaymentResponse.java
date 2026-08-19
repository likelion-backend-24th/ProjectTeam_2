package org.example.backend.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * PortOne V2 "결제 단건 조회" (GET /payments/{paymentId}) 응답 중
 * 서버 검증에 필요한 필드만 파싱한다.
 * status 예시: READY, PAID, FAILED, CANCELLED, PARTIAL_CANCELLED, VIRTUAL_ACCOUNT_ISSUED, PAY_PENDING
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
        String status,
        String id,
        String storeId,
        Channel channel,
        String currency,
        Amount amount,
        Instant paidAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(
            String type,
            String id,
            String key
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(
            Long total,
            Long paid
    ) {
    }
}
