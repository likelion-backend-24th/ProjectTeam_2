package org.example.backend.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.payment.dto.response.PaymentInfoResponse;
import org.example.backend.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "정기결제 해지 예약", description = "다음 자동결제를 취소합니다. 이미 결제된 기간은 만료일까지 그대로 이용 가능합니다.")
    @DeleteMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> cancelAutoRenewal(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        paymentService.cancelAutoRenewal(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("다음 결제가 취소되었습니다. 이용 기간까지는 계속 이용 가능합니다.", null));
    }

    // 취소했던 다음 자동결제를 같은 예정일로 다시 예약
    @Operation(summary = "정기결제 재개", description = "취소했던 다음 자동결제를 원래 예정일 그대로 다시 예약합니다.")
    @PostMapping("/subscribe/resume")
    public ResponseEntity<ApiResponse<Void>> resumeAutoRenewal(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        paymentService.resumeAutoRenewal(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("정기결제가 다시 예약되었습니다.", null));
    }


    // 내 정기결제 예약 상태 조회
    @Operation(summary = "내 정기결제 예약 조회", description = "다음 결제 예정일과 자동갱신 여부를 조회합니다.")
    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<PaymentInfoResponse>> getMySchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaymentInfoResponse response = paymentService.getMySchedule(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("정기결제 예약 조회 성공", response));
    }
}