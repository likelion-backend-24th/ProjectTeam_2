package org.example.backend.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.payment.entity.WebhookEvent;
import org.example.backend.payment.entity.WebhookEventStatus;
import org.example.backend.payment.repository.WebhookEventRepository;
import org.example.backend.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// 정기결제 웹훅 수신 API
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;
    private final WebhookEventRepository webhookEventRepository;

    //Json 텍스트 파싱 도구
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${portone.webhook-secret}")
    private String webhookSecret;

    // PortOne이 자동결제 결과를 알려주는 진입점
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "webhook-id", required = false) String webhookId,
            @RequestHeader(value = "webhook-signature", required = false) String webhookSignature,
            @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp
    ) {
        // 서명 헤더가 다 있으면 검증, 하나라도 없으면 검증 없이 진행 (재조회로 어차피 다시 확인함)
        if (webhookId == null || webhookSignature == null || webhookTimestamp == null) {
            log.warn("웹훅 서명 헤더 없음, 서명 검증 없이 진행함");
        } else {
            try {
                WebhookVerifier verifier = new WebhookVerifier(webhookSecret);
                verifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);
            } catch (Exception e) {
                log.warn("웹훅 서명 검증 실패: {}", e.getMessage());
                return ResponseEntity.badRequest().build();
            }
        }

        // webhookId가 없으면(서명 없는 경우) 중복 체크는 건너뜀
        if (webhookId != null && webhookEventRepository.existsByWebhookId(webhookId)) {
            log.info("이미 처리된 웹훅입니다: {}", webhookId);
            return ResponseEntity.ok().build();
        }

        String eventType = "UNKNOWN";
        String paymentId = null;
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            eventType = root.path("type").asText("UNKNOWN");
            paymentId = root.path("data").path("paymentId").asText(null);
        } catch (Exception e) {
            log.error("웹훅 본문 파싱 실패: {}", e.getMessage());
        }

        WebhookEvent event = new WebhookEvent();
        event.setWebhookId(webhookId != null ? webhookId : "unverified-" + UUID.randomUUID());
        event.setEventType(eventType);
        event.setPaymentId(paymentId);
        event.setRawPayload(rawBody);
        event.setStatus(WebhookEventStatus.RECEIVED);
        webhookEventRepository.save(event);

        if (paymentId != null) {
            try {
                paymentService.handleWebhook(paymentId);
                event.setStatus(WebhookEventStatus.PROCESSED);
            } catch (Exception e) {
                log.error("웹훅 처리 중 오류: {}", e.getMessage());
            }
        } else {
            event.setStatus(WebhookEventStatus.IGNORED);
        }
        webhookEventRepository.save(event);

        return ResponseEntity.ok().build();
    }
}