package org.example.backend.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.payment.converter.BillingKeyConverter;
import org.example.backend.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_key")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = BillingKeyConverter.class)
    @Column(name = "billing_key_token", nullable = false, length = 500)
    private String billingKeyToken;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public BillingKey(User user, String billingKeyToken, LocalDateTime issuedAt) {
        this.user = user;
        this.billingKeyToken = billingKeyToken;
        this.issuedAt = issuedAt;
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
