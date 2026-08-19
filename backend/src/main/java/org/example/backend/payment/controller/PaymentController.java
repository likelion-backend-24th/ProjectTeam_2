package org.example.backend.payment.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.payment.dto.PaymentCompleteRequest;
import org.example.backend.payment.dto.PaymentPrepareRequest;
import org.example.backend.payment.dto.PaymentPrepareResponse;
import org.example.backend.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "결제", description = "PortOne 결제 준비/완료 API")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> preparePayment(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody PaymentPrepareRequest request
    ){
        PaymentPrepareResponse response = paymentService.preparePayment(user.getUser(), request.getPlanType());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("결제 준비가 완료되었습니다.", response));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Void>> completePayment(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody PaymentCompleteRequest request
    ) {
        paymentService.completePayment(user.getUser(), request.getPaymentId());
        return ResponseEntity.ok(ApiResponse.success("결제가 완료되었습니다.", null));
    }
}
