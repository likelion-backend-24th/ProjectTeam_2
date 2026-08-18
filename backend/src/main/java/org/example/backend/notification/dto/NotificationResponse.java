package org.example.backend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.notification.entity.Notification;
import org.example.backend.notification.entity.NotificationTargetType;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    @Schema(description = "알림 ID", example = "1")
    private Long id;

    @Schema(description = "알림 대상 종류", example = "POST")
    private NotificationTargetType targetType;

    @Schema(description = "대상 게시글/스터디게시글 ID", example = "5")
    private Long targetId;

    @Schema(description = "댓글 ID", example = "12")
    private Long commentId;

    @Schema(description = "댓글 미리보기(최대 30자)", example = "저도 이 회사 면접 본 적 있는데...")
    private String commentPreview;

    @Schema(description = "읽음 여부", example = "false")
    private boolean isRead;

    @Schema(description = "알림 생성 시각", example = "2026-08-18T10:00:00")
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .commentId(notification.getCommentId())
                .commentPreview(notification.getCommentPreview())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}