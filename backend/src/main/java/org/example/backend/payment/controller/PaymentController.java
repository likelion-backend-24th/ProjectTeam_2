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
import org.springframework.web.bind.annotation.*;

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
        paymentService.completePayment(user.getUser(), request.getBillingKey(), request.getPlanType());
        return ResponseEntity.ok(ApiResponse.success("결제가 완료되었습니다.", null));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String body,   // JSON을 DTO로 파싱했다 다시 만들면 바이트가 달라져 검증이 깨지기 쉬우므로 String 그대로 받아옴.
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String signature,
            @RequestHeader("webhook-timestamp") String timestamp
    ) {
        paymentService.handleWebhook(body, webhookId, signature, timestamp);
        return ResponseEntity.ok().build();
    }
}
