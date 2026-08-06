package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.FeedbackMessage;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackMessageResponse {
    private Long id;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;

    public static FeedbackMessageResponse from(FeedbackMessage message) {
        return FeedbackMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}