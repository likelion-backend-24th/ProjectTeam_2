package org.example.backend.subscription.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionResponse {
    @Schema(description = "구독 상태 (ACTIVE, CANCELLED)", example = "ACTIVE")
    private SubscriptionStatus status;

    @Schema(description = "구독 시작일시", example = "2026-08-13T10:00:00")
    private LocalDateTime startedAt;

    @Schema(description = "구독 만료 예정일시", example = "2026-09-13T10:00:00")
    private LocalDateTime expiredAt;

    public static SubscriptionResponse from(Subscription subscription) {
        return SubscriptionResponse.builder()
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiredAt(subscription.getExpiredAt())
                .build();
    }
}