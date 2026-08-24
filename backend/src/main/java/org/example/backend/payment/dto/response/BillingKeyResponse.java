package org.example.backend.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.payment.entity.BillingKey;

import java.time.LocalDateTime;

// 등록된 카드 하나에 대한 응답 (마이페이지 카드 목록용)
@Getter
@Builder
public class BillingKeyResponse {

    @Schema(description = "카드 고유 ID (선택/삭제 요청 시 사용)", example = "3")
    private Long id;

    @Schema(description = "화면 표시용 카드 이름 (등록 순서 기준)", example = "카드 2")
    private String label;

    @Schema(description = "카드 등록일시", example = "2026-08-20T10:00:00")
    private LocalDateTime registeredAt;

    @Schema(description = "현재 결제에 사용되는 선택된 카드인지 여부", example = "true")
    private Boolean selected;

    public static BillingKeyResponse from(BillingKey billingKey, int order) {
        return BillingKeyResponse.builder()
                .id(billingKey.getId())
                .label("카드 " + order)
                .registeredAt(billingKey.getCreatedAt())
                .selected(billingKey.getIsSelected())
                .build();
    }
}