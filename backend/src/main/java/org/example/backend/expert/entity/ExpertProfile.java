package org.example.backend.expert.entity;

import org.example.backend.expert.exception.InvalidExpertStatusException;
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

    public void approve() {
        if (this.status != ExpertStatus.PENDING) {
            throw new InvalidExpertStatusException("PENDING 상태의 신청만 승인할 수 있습니다.");
        }
        this.status = ExpertStatus.APPROVED;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        if (this.status != ExpertStatus.PENDING) {
            throw new InvalidExpertStatusException("PENDING 상태의 신청만 거절할 수 있습니다.");
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void revoke(String reason) {
        if (this.status != ExpertStatus.APPROVED) {
            throw new InvalidExpertStatusException("APPROVED 상태의 전문가만 자격 박탈할 수 있습니다.");
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void reapply(String career, String certification) {
        if (this.status != ExpertStatus.REJECTED) {
            throw new InvalidExpertStatusException("거절된 신청만 재신청할 수 있습니다.");
        }
        this.career = career;
        this.certification = certification;
        this.status = ExpertStatus.PENDING;
        this.rejectReason = null;
    }

    public boolean isApproved() {
        return this.status == ExpertStatus.APPROVED;
    }
}