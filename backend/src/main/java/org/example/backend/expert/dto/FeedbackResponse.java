package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Feedback;
import org.example.backend.expert.entity.FeedbackStatus;

import java.time.LocalDateTime;

/**
 * 문의 스레드(Feedback) 자체의 기본 정보를 담는 응답 DTO.
 */


@Getter
@Builder
public class FeedbackResponse {
    private Long id;
    private Long requesterId;
    private Long expertProfileId;
    private FeedbackStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .requesterId(feedback.getRequester().getId())
                .expertProfileId(feedback.getExpertProfile().getId())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .answeredAt(feedback.getAnsweredAt())
                .build();
    }
}