package org.example.backend.payment.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String body, // DTO로 파싱했다 재조립하면 바이트가 달라져 서명 검증이 깨짐 - 원문 그대로 받음
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String signature,
            @RequestHeader("webhook-timestamp") String timestamp
    ) {
        paymentService.handleWebhook(body, webhookId, signature, timestamp);
        return ResponseEntity.ok().build();
    }
}
