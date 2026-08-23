package org.example.backend.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "구독", description = "유료 구독 신청/취소/조회 API")
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "구독 신청", description = "프론트에서 결제 성공 처리 후 호출. 현재는 실제 PG 연동 없이 상태만 반영함.")
    @PostMapping("/api/subscriptions")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = subscriptionService.subscribe(userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("구독이 시작되었습니다.", response));
    }

    @Operation(summary = "구독 해지", description = "다음 회차 자동 갱신만 멈춘다. 만료일까지는 계속 이용할 수 있다.")
    @DeleteMapping("/api/subscriptions")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = subscriptionService.cancel(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("자동 갱신이 해지되었습니다.", response));
    }

    @Operation(summary = "구독 재개", description = "해지 예약 상태에서 자동 갱신을 다시 켠다.")
    @PostMapping("/api/subscriptions/resume")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> resume(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = subscriptionService.resume(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("자동 갱신이 재개되었습니다.", response));
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