package org.example.backend.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.payment.dto.PaymentPrepareResponse;
import org.example.backend.subscription.dto.request.SubscriptionCardChangeRequest;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "유예기간(PAST_DUE) 중 스케줄러를 기다리지 않고 지금 바로 등록된 카드로 재시도")
    @PostMapping("/api/subscriptions/retry")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> retryPastDueNow(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = subscriptionService.retryPastDueNow(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("재시도가 완료되었습니다.", response));
    }

    @Operation(summary = "해지 예약 취소(자동갱신 재개) - 해지 시 지우지 않은 카드를 그대로 되살린다. 새 카드 등록/결제 없음")
    @PostMapping("/api/subscriptions/resume")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> resume(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = subscriptionService.resume(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("해지 예약이 취소되었습니다.", response));
    }

    @Operation(summary = "결제수단 변경 준비 - 새 빌링키 발급에 필요한 값 반환. 이미 자동갱신 중인 경우에만 가능(새 결제는 발생하지 않음)")
    @PostMapping("/api/subscriptions/card/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepareCardChange(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaymentPrepareResponse response = subscriptionService.prepareCardChange(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("결제수단 변경 준비가 완료되었습니다.", response));
    }

    @Operation(summary = "결제수단 변경 - prepare에서 발급받은 빌링키로 기존 카드를 교체. 새 결제는 발생하지 않음")
    @PostMapping("/api/subscriptions/card")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> changeCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SubscriptionCardChangeRequest request
    ) {
        SubscriptionResponse response = subscriptionService.changeCard(userDetails.getUser().getId(), request.getBillingKey());
        return ResponseEntity.ok(ApiResponse.success("결제수단이 변경되었습니다.", response));
    }
}