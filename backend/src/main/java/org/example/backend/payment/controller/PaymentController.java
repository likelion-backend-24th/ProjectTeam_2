package org.example.backend.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 정기결제(빌링키 기반) API
@Tag(name = "결제", description = "정기결제 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 등록된 빌링키로 첫 결제 실행 (카드 등록은 이미 완료된 상태여야 함)
    @Operation(summary = "첫 결제 실행", description = "등록된 빌링키로 첫 결제를 실행하고 구독을 활성화합니다.")
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        paymentService.chargeFirstPayment(customUserDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("구독이 시작되었습니다.", null));
    }
}