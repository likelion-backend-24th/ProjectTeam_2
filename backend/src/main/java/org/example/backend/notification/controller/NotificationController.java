package org.example.backend.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.common.dto.Meta;
import org.example.backend.common.dto.PageMeta;
import org.example.backend.notification.dto.NotificationCountResponse;
import org.example.backend.notification.dto.NotificationResponse;

import org.example.backend.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "댓글 알림 조회 API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "안 읽은 알림 개수 조회", description = "우상단 뱃지에 표시할 안 읽은 알림 개수를 조회합니다.")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<NotificationCountResponse>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NotificationCountResponse response = notificationService.getUnreadCount(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("안 읽은 알림 개수 조회 성공", response));
    }

    @Operation(summary = "알림 목록 조회", description = "내 알림 목록을 조회합니다. 조회하는 시점에 안 읽은 알림이 전부 읽음 처리됩니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<NotificationResponse> page = notificationService.getNotifications(userDetails.getUser().getId(), pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("알림 목록 조회 성공", page.getContent(), meta));
    }
}