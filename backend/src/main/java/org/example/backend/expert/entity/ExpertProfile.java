package org.example.backend.expert.entity;

import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expert_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // users 테이블 FK, unique 제약 -> 1:1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "career", columnDefinition = "TEXT")
    private String career;

    @Column(name = "certification", columnDefinition = "TEXT")
    private String certification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpertStatus status;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Builder
    public ExpertProfile(User user, String career, String certification) {
        this.user = user;
        this.career = career;
        this.certification = certification;
        this.status = ExpertStatus.PENDING;
    }

    // F-26: ADMIN이 career/certification 검토 후 승인. 명세서 사전조건: 대상이 PENDING 상태
    public void approve() {
        if (this.status != ExpertStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 신청만 승인할 수 있습니다.");
        }
        this.status = ExpertStatus.APPROVED;
        this.rejectReason = null;
    }

    // F-26: ADMIN이 career/certification 검토 후 거절. 명세서 사전조건: 대상이 PENDING 상태
    public void reject(String reason) {
        if (this.status != ExpertStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 신청만 거절할 수 있습니다.");
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    // F-27: ADMIN 자격 박탈 (APPROVED 상태의 전문가만 대상)
    public void revoke(String reason) {
        if (this.status != ExpertStatus.APPROVED) {
            throw new IllegalStateException("APPROVED 상태의 전문가만 자격 박탈할 수 있습니다.");
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    public boolean isApproved() {
        return this.status == ExpertStatus.APPROVED;
    }
}