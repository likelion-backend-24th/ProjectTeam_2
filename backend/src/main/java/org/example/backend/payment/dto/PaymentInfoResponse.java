package org.example.backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 내 정기결제 예약 상태 조회 응답
@Getter
@Builder
public class PaymentInfoResponse {

    @Schema(description = "다음 자동결제 예정일", example = "2026-10-23T10:00:00")
    private LocalDateTime nextChargeAt;

    @Schema(description = "다음 결제도 자동으로 갱신될지 여부", example = "true")
    private Boolean autoRenew;
}