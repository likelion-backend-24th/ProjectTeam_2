package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {
    @Schema(description = "문의 스레드 ID", example = "1")
    private Long id;

    @Schema(description = "문의를 요청한 유저 ID", example = "3")
    private Long requesterId;

    @Schema(description = "담당 전문가 프로필 ID", example = "1")
    private Long expertProfileId;

    @Schema(description = "문의 주제", example = "이력서 첨삭 부탁드립니다")
    private String topic;

    @Schema(description = "답변 상태 (PENDING: 답변 대기, ANSWERED: 답변 완료)", example = "PENDING")
    private FeedbackStatus status;

    @Schema(description = "문의 개설일시", example = "2026-08-05T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "문의 답변일시", example = "2026-08-05T12:00:00")
    private LocalDateTime answeredAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .requesterId(feedback.getRequester().getId())
                .expertProfileId(feedback.getExpertProfile().getId())
                .topic(feedback.getTopic())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .answeredAt(feedback.getAnsweredAt())
                .build();
    }
}