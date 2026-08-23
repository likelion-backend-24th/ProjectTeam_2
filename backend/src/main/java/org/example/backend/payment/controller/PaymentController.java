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

    @Operation(summary = "정기결제 재시도", description = "결제 실패(PAST_DUE) 상태에서 등록된 카드로 즉시 재결제를 시도한다.")
    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> retry(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionResponse response = paymentService.retrySubscriptionPayment(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("재결제를 시도했습니다.", response));
    }
}
