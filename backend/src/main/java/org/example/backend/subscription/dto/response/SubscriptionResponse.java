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
    @Schema(description = "구독 상태 (ACTIVE, PAST_DUE, EXPIRED)", example = "ACTIVE")
    private SubscriptionStatus status;

    @Schema(description = "구독 시작일시", example = "2026-08-13T10:00:00")
    private LocalDateTime startedAt;

    @Schema(description = "구독 만료(또는 다음 결제) 예정일시", example = "2026-09-13T10:00:00")
    private LocalDateTime expiredAt;

    @Schema(description = "다음 회차 자동 결제 여부", example = "true")
    private boolean autoRenew;

    @Schema(description = "결제 실패(PAST_DUE) 상태에서 만료까지 남은 자동 재시도 횟수. "
            + "0이 되는 시점에 이용이 중단된다. 재시도는 하루 한 번.", example = "2")
    private int remainingRetryCount;

    public static SubscriptionResponse from(Subscription subscription) {
        return SubscriptionResponse.builder()
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiredAt(subscription.getExpiredAt())
                .autoRenew(subscription.isAutoRenew())
                .remainingRetryCount(subscription.getRemainingRetryCount())
                .build();
    }
}