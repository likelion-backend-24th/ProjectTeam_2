package org.example.backend.payment.repository;

import org.example.backend.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 웹훅 수신 기록 저장/조회
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByWebhookId(String webhookId);
}