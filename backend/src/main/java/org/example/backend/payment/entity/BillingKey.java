package org.example.backend.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.payment.converter.BillingKeyConverter;
import org.example.backend.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 검증 완료된 빌링키 (암호화 저장)
@Entity
@Table(name = "billing_key")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillingKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = BillingKeyConverter.class) // 저장할땐 암호화 조회할땐 복호화
    @Column(name = "billing_key", nullable = false, length = 500)
    private String billingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillingKeyStatus status;

    // 지금 결제/예약에 사용할 카드인지 여부 (유저당 ACTIVE 상태에서 정확히 1개만 true)
    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}