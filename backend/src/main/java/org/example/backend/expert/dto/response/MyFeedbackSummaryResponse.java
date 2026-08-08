package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyFeedbackSummaryResponse {

    @Schema(description = "문의 스레드 ID", example = "1")
    private Long feedbackId;

    @Schema(description = "담당 전문가 닉네임", example = "전문개발자")
    private String expertNickname;

    @Schema(description = "문의 주제", example = "이력서 첨삭 부탁드립니다")
    private String topic;

    @Schema(description = "답변 상태 (PENDING: 답변 대기, ANSWERED: 답변 완료)", example = "PENDING")
    private FeedbackStatus status;

    @Schema(description = "전문가 최초 답변일시 (답변 전이면 null)", example = "null")
    private LocalDateTime answeredAt;

    public static MyFeedbackSummaryResponse from(Feedback feedback) {
        return MyFeedbackSummaryResponse.builder()
                .feedbackId(feedback.getId())
                .expertNickname(feedback.getExpertProfile().getUser().getNickname())
                .topic(feedback.getTopic())
                .status(feedback.getStatus())
                .answeredAt(feedback.getAnsweredAt())
                .build();
    }
}