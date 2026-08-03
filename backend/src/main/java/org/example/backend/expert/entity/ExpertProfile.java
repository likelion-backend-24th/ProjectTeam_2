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

    // F-27: ADMIN 자격 박탈 (승인된 전문가를 다시 거절 상태로).
    // 명세서엔 명시 안 돼있지만, 이미 APPROVED인 전문가만 박탈 대상이 되는 게 자연스러워서 추가한 가드.
    public void revoke(String reason) {
        if (this.status != ExpertStatus.APPROVED) {
            throw new IllegalStateException("APPROVED 상태의 전문가만 자격 박탈할 수 있습니다.");
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    // F-25: 거절(REJECTED)된 신청만 재신청 가능. 새 row를 만들지 않고 기존 row를 재사용
    // (user_id UNIQUE 제약 때문에 어차피 새로 못 만듦. career/certification을 새 값으로 갱신하고 PENDING으로 되돌림)
    public void reapply(String career, String certification) {
        if (this.status != ExpertStatus.REJECTED) {
            throw new IllegalStateException("거절된 신청만 재신청할 수 있습니다.");
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