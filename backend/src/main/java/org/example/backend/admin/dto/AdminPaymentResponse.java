package org.example.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.payment.entity.Payment;
import org.example.backend.payment.entity.PaymentPurpose;
import org.example.backend.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPaymentResponse {
    @Schema(description = "결제 ID (PK)", example = "1")
    private Long id;
    @Schema(description = "PortOne 결제 ID", example = "p2g-1a2b3c4d")
    private String paymentId;
    @Schema(description = "유저 ID", example = "1")
    private Long userId;
    @Schema(description = "유저 닉네임", example = "전주족발집알바생")
    private String userNickname;
    @Schema(description = "유저 이메일", example = "kjs@naver.com")
    private String userUsername;
    @Schema(description = "결제 목적", example = "SUBSCRIPTION")
    private PaymentPurpose purpose;
    @Schema(description = "결제 금액", example = "9900")
    private Long amount;
    @Schema(description = "통화", example = "KRW")
    private String currency;
    @Schema(description = "주문명", example = "프리미엄 구독 1개월")
    private String orderName;
    @Schema(description = "결제 상태", example = "PAID")
    private PaymentStatus status;
    @Schema(description = "실패 사유 (실패한 경우만)", example = "null")
    private String failReason;
    @Schema(description = "결제 요청일시", example = "2026-08-05T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "결제 완료일시 (완료 안 됐으면 null)", example = "2026-08-05T10:00:05")
    private LocalDateTime paidAt;

    public static AdminPaymentResponse from(Payment payment) {
        return AdminPaymentResponse.builder()
                .id(payment.getId())
                .paymentId(payment.getPaymentId())
                .userId(payment.getUser().getId())
                .userNickname(payment.getUser().getNickname())
                .userUsername(payment.getUser().getUsername())
                .purpose(payment.getPurpose())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .orderName(payment.getOrderName())
                .status(payment.getStatus())
                .failReason(payment.getFailReason())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
