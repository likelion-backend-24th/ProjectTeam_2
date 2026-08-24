package org.example.backend.payment.repository;

import org.example.backend.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByWebhookId(String webhookId);
}
