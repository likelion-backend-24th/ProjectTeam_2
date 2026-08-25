package org.example.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminSubscriptionResponse {
    @Schema(description = "구독 ID", example = "1")
    private Long id;
    @Schema(description = "유저 ID", example = "1")
    private Long userId;
    @Schema(description = "유저 닉네임", example = "전주족발집알바생")
    private String userNickname;
    @Schema(description = "유저 이메일", example = "kjs@naver.com")
    private String userUsername;
    @Schema(description = "구독 상태", example = "ACTIVE")
    private SubscriptionStatus status;
    @Schema(description = "구독 시작일시", example = "2026-08-05T10:00:00")
    private LocalDateTime startedAt;
    @Schema(description = "구독 만료일시 (자동갱신 성공 시 매달 갱신됨)", example = "2026-09-05T10:00:00")
    private LocalDateTime expiredAt;
    @Schema(description = "자동 갱신 여부", example = "true")
    private boolean autoRenew;
    @Schema(description = "연속 결제 실패 횟수 (3회 소진 시 만료)", example = "0")
    private int retryCount;

    public static AdminSubscriptionResponse from(Subscription subscription) {
        return AdminSubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .userNickname(subscription.getUser().getNickname())
                .userUsername(subscription.getUser().getUsername())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiredAt(subscription.getExpiredAt())
                .autoRenew(subscription.isAutoRenew())
                .retryCount(subscription.getRetryCount())
                .build();
    }
}
