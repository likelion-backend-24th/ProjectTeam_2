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
    @Schema(description = "구독 상태 (ACTIVE, PAST_DUE, CANCELLED)", example = "ACTIVE")
    private SubscriptionStatus status;

    @Schema(description = "구독 시작일시", example = "2026-08-13T10:00:00")
    private LocalDateTime startedAt;

    @Schema(description = "구독 만료 예정일시", example = "2026-09-13T10:00:00")
    private LocalDateTime expiredAt;

    @Schema(description = "다음 결제일에 자동 갱신되는지 여부 (false면 해지 예약 상태 - expiredAt까지만 이용 가능)", example = "true")
    private boolean autoRenew;

    @Schema(description = "유예기간(PAST_DUE) 마감 시점 - status가 PAST_DUE일 때만 값이 있음. 이 시점 전까지 결제수단을 고치면 구독이 복구됨", example = "2026-09-16T10:00:00")
    private LocalDateTime graceEndsAt;

    public static SubscriptionResponse from(Subscription subscription, boolean autoRenew) {
        return SubscriptionResponse.builder()
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiredAt(subscription.getExpiredAt())
                .autoRenew(autoRenew)
                .graceEndsAt(subscription.getGraceEndsAt())
                .build();
    }
}