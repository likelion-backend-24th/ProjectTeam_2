package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyFeedbackSummaryResponse {
    private Long feedbackId;
    private String expertNickname;
    private String topic;
    private FeedbackStatus status;
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