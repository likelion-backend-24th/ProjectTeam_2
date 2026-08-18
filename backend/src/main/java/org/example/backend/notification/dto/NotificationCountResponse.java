package org.example.backend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationCountResponse {

    @Schema(description = "안 읽은 알림 개수", example = "3")
    private long unreadCount;
}