package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.FeedbackMessage;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Builder
public class FeedbackMessageResponse {
    @Schema(description = "메시지 ID", example = "1")
    private Long id;

    @Schema(description = "보낸 사람 유저 ID (요청자 또는 담당 전문가)", example = "3")
    private Long senderId;

    @Schema(description = "메시지 내용", example = "확인 후 다시 연락드릴게요.")
    private String content;

    @Schema(description = "첨부 이미지 URL 목록")
    private List<String> imageUrls;

    @Schema(description = "메시지 전송일시", example = "2026-08-05T11:00:00")
    private LocalDateTime createdAt;

    public static FeedbackMessageResponse from(FeedbackMessage message) {
        return from(message, List.of());
    }

    public static FeedbackMessageResponse from(FeedbackMessage message, List<String> imageUrls) {
        return FeedbackMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .content(message.getContent())
                .imageUrls(imageUrls)
                .createdAt(message.getCreatedAt())
                .build();
    }
}