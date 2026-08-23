package org.example.backend.payment.entity;

import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true, length = 100)
    private String paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private PaymentPurpose purpose;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "order_name", nullable = false, length = 100)
    private String orderName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id")
    private BillingKey billingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "fail_reason", length = 255)
    private String failReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    public Payment(String paymentId, User user, PaymentPurpose purpose, Long amount, String currency,
                   String orderName, BillingKey billingKey) {
        this.paymentId = paymentId;
        this.user = user;
        this.purpose = purpose;
        this.amount = amount;
        this.currency = currency;
        this.orderName = orderName;
        this.billingKey = billingKey;
        this.status = PaymentStatus.READY;
        this.createdAt = LocalDateTime.now();
    }

    public void markPaid(LocalDateTime paidAt) {
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
    }

    public boolean isReady() {
        return this.status == PaymentStatus.READY;
    }

    public boolean belongsTo(Long userId) {
        return this.user.getId().equals(userId);
    }
}
