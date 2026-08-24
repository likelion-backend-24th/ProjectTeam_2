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

    /** 카드전표인자명(예: 신한카드). 표시 전용이며 PG가 주지 않으면 null이다. */
    @Column(name = "card_name", length = 50)
    private String cardName;

    /** 마스킹된 카드번호. 표시 전용이며 PG가 주지 않으면 null이다. */
    @Column(name = "card_number_masked", length = 30)
    private String cardNumberMasked;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public BillingKey(User user, String billingKeyToken, String cardName, String cardNumberMasked,
                      LocalDateTime issuedAt) {
        this.user = user;
        this.billingKeyToken = billingKeyToken;
        this.cardName = cardName;
        this.cardNumberMasked = cardNumberMasked;
        this.issuedAt = issuedAt;
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
