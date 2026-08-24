package org.example.backend.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.payment.dto.request.PaymentCompleteRequest;
import org.example.backend.payment.dto.response.PaymentReadyResponse;
import org.example.backend.payment.service.PaymentService;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "결제", description = "PortOne V2 기반 구독 결제 API")
@RestController
@RequestMapping("/api/payments/subscriptions")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "구독 결제 준비", description = "서버가 금액을 확정하고 paymentId를 발급한다. 프론트는 이 값으로 PortOne 결제창을 호출한다.")
    @PostMapping("/ready")
    public ResponseEntity<ApiResponse<PaymentReadyResponse>> ready(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaymentReadyResponse response = paymentService.readySubscriptionPayment(userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("결제 준비가 완료되었습니다.", response));
    }

    @Operation(summary = "구독 결제 완료 검증", description = "PortOne 결제 단건 조회로 실제 결제 결과를 검증한 뒤 통과 시에만 구독을 시작한다.")
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> complete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentCompleteRequest request
    ) {
        SubscriptionResponse response = paymentService.completeSubscriptionPayment(
                userDetails.getUser().getId(), request.paymentId());
        return ResponseEntity.ok(ApiResponse.success("구독이 시작되었습니다.", response));
    }

    @Operation(summary = "정기결제 구독 시작", description = "등록된 빌링키로 즉시 청구하고, 성공하면 다음 회차부터 자동 갱신되는 구독을 시작한다.")
    @PostMapping("/billing-key")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribeWithBillingKey(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = paymentService.subscribeWithBillingKey(userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("구독이 시작되었습니다.", response));
    }

    @Operation(summary = "자동 갱신 재개",
            description = "해지 예약 상태에서 다음 회차부터 다시 자동 결제한다. 등록된 카드가 없으면 재개할 수 없다.")
    @PostMapping("/resume")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> resume(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = paymentService.resumeAutoRenew(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("자동 갱신이 재개되었습니다.", response));
    }

    @Operation(summary = "정기결제 재시도",
            description = "결제 실패(PAST_DUE) 상태에서 등록된 카드로 즉시 재결제를 시도한다. "
                    + "실패해도 자동 재시도 횟수는 줄지 않으며, 직전 청구로부터 1분이 지나야 다시 호출할 수 있다.")
    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> retry(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = paymentService.retrySubscriptionPayment(userDetails.getUser().getId());
        // 이 API는 PAST_DUE에서만 호출되므로, 돌아온 상태가 ACTIVE면 이번 시도가 성공한 것이다.
        // 실패를 예외로 알리면 방금 남긴 FAILED 결제 기록까지 롤백되므로 메시지로만 구분한다.
        boolean succeeded = response.getStatus() == SubscriptionStatus.ACTIVE;
        return ResponseEntity.ok(succeeded
                ? ApiResponse.success("결제가 완료되었습니다.", response)
                : ApiResponse.success("결제에 실패했습니다. 카드 상태를 확인한 뒤 다시 시도해주세요.", response));
    }
}
