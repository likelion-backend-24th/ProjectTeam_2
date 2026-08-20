package org.example.backend.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_event")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "webhook_id", nullable = false, unique = true, length = 100)
    private String webhookId;

    @CreatedDate
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    public WebhookEvent(String webhookId) {
        this.webhookId = webhookId;
    }
}
