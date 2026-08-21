package org.example.backend.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "구독", description = "유료 구독 조회/해지 API")
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "구독 해지 예약 (다음 자동갱신부터 중단, 남은 기간은 계속 이용 가능)")
    @DeleteMapping("/api/subscriptions")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = subscriptionService.cancel(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("구독 해지가 예약되었습니다.", response));
    }

    @Operation(summary = "내 구독 조회")
    @GetMapping("/api/subscriptions/me")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMy(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = subscriptionService.getMy(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("구독 조회 성공", response));
    }
}