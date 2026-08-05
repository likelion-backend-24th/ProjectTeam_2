package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackStatus;

import java.time.LocalDateTime;

/**
 * "내 문의 스레드 목록"(GET /api/feedbacks/me) 응답의 개별 항목.
 */


@Getter
@Builder
public class MyFeedbackSummaryResponse {
    private Long feedbackId;
    private String expertNickname;
    private FeedbackStatus status;
    private LocalDateTime answeredAt;

    public static MyFeedbackSummaryResponse from(Feedback feedback) {
        return MyFeedbackSummaryResponse.builder()
                .feedbackId(feedback.getId())
                .expertNickname(feedback.getExpertProfile().getUser().getNickname())
                .status(feedback.getStatus())
                .answeredAt(feedback.getAnsweredAt())
                .build();
    }
}