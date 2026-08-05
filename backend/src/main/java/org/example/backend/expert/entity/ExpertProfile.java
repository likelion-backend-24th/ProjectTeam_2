package org.example.backend.expert.entity;

import org.example.backend.expert.exception.InvalidExpertStatusException;
import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * 전문가 신청/심사 정보 (F-25~F-27, F-32).
 * User와 1:1 관계, 신청 시점에 생성되고 status를 통해
 * PENDING(심사 대기) -> APPROVED(승인) / REJECTED(거절 또는 자격 박탈) 구조.
 * 자격 박탈은 별도 상태값을 두지 않고 REJECTED를 재사용!
 */

@Entity
@Table(name = "expert_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청한 유저. user_id UNIQUE 제약으로 한 유저당 프로필 1개만
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 경력.
    @Column(name = "career", columnDefinition = "TEXT")
    private String career;

    // 자격증. 승인 여부를 판단하는 기준은 아님.
    @Column(name = "certification", columnDefinition = "TEXT")
    private String certification;

    // 심사 상태. PENDING/APPROVED/REJECTED
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpertStatus status;

    // 거절/박탈 사유.
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    // 상태는 항상 PENDING 시작.
    @Builder
    public ExpertProfile(User user, String career, String certification) {
        this.user = user;
        this.career = career;
        this.certification = certification;
        this.status = ExpertStatus.PENDING;
    }

    /**
     * 관리자가 신청을 승인. PENDING 상태의 신청만 승인할 수 있으며,
     * 승인되면 이전에 남아있던 거절 사유는 null로 변경.
     */
    public void approve() {
        if (this.status != ExpertStatus.PENDING) {
            throw new InvalidExpertStatusException("PENDING 상태의 신청만 승인할 수 있습니다.");
        }
        this.status = ExpertStatus.APPROVED;
        this.rejectReason = null;
    }

    /**
     * 관리자가 신청을 거절. PENDING 상태의 신청만 거절할 수 있고,
     * 거절 사유를 함께 저장. 거절된 신청은 재신청이 가능.
     */
    public void reject(String reason) {
        if (this.status != ExpertStatus.PENDING) {
            throw new InvalidExpertStatusException("PENDING 상태의 신청만 거절할 수 있습니다.");
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    /**
     * 이미 승인된 전문가의 자격을 박탈. APPROVED 상태에서만 호출 가능하며,
     * 박탈 후 상태는 REJECTED로 전환.
     */
    public void revoke(String reason) {
        if (this.status != ExpertStatus.APPROVED) {
            throw new InvalidExpertStatusException("APPROVED 상태의 전문가만 자격 박탈할 수 있습니다.");
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    /**
     * 거절 이력이 있는 유저가 경력/자격증 정보를 갱신해 재신청.
     * REJECTED 상태에서만 호출할 수 있으며, 재신청 시 상태는 다시 PENDING으로,
     * 거절 사유는 null로 초기화.
     */
    public void reapply(String career, String certification) {
        if (this.status != ExpertStatus.REJECTED) {
            throw new InvalidExpertStatusException("거절된 신청만 재신청할 수 있습니다.");
        }
        this.career = career;
        this.certification = certification;
        this.status = ExpertStatus.PENDING;
        this.rejectReason = null;
    }

    // 현재 승인된 전문가인지 여부 체크용 메서드.
    public boolean isApproved() {
        return this.status == ExpertStatus.APPROVED;
    }
}