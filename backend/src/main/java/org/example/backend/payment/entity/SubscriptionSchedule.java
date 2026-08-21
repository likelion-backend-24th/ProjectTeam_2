package org.example.backend.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 구독 회차와 재시도별 결제 실행 예약 상태 (다음 자동결제 예약 기록)
@Entity
@Table(name = "subscription_schedule")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id", nullable = false)
    private BillingKey billingKey;

    @Column(name = "next_payment_id", nullable = false, unique = true, length = 100)
    private String nextPaymentId;  //다음 회차 결제 paymentId

    @Column(name = "next_charge_at", nullable = false)
    private LocalDateTime nextChargeAt; //다음 결제 예정 시각

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew; //다음회차도 자동갱신 할지

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}