package org.example.backend.expert.entity;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.expert.exception.ExpertErrorCode;
import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전문가 신청/심사 정보를 담는 엔티티 (F-25~F-27, F-32).
 */
@Entity
@Table(name = "expert_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청한 유저. user_id UNIQUE 제약으로 한 유저당 프로필 1개만 가질 수 있다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 경력(년수). 신청 시 입력값.
    @Column(name = "career", columnDefinition = "TEXT")
    private String career;

    // 자격증. 프로필 노출용 정보이며 승인 여부를 판단하는 기준으로는 쓰이지 않는다(F-25).
    @Column(name = "certification", columnDefinition = "TEXT")
    private String certification;

    // 심사 상태. PENDING/APPROVED/REJECTED 세 가지 값을 가진다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpertStatus status;

    // 거절/박탈 사유. 승인 시에는 다시 null로 초기화된다.
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    // 신청 시 호출되는 생성자. 상태는 항상 PENDING으로 시작한다.
    @Builder
    public ExpertProfile(User user, String career, String certification) {
        this.user = user;
        this.career = career;
        this.certification = certification;
        this.status = ExpertStatus.PENDING;
    }

    public void approve() {
        if (this.status != ExpertStatus.PENDING) {
            throw new BusinessException(ExpertErrorCode.EXPERT_APPROVE_INVALID_STATUS);
        }
        this.status = ExpertStatus.APPROVED;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        if (this.status != ExpertStatus.PENDING) {
            throw new BusinessException(ExpertErrorCode.EXPERT_REJECT_INVALID_STATUS);
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void revoke(String reason) {
        if (this.status != ExpertStatus.APPROVED) {
            throw new BusinessException(ExpertErrorCode.EXPERT_REVOKE_INVALID_STATUS);
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void reapply(String career, String certification) {
        if (this.status != ExpertStatus.REJECTED) {
            throw new BusinessException(ExpertErrorCode.EXPERT_REAPPLY_INVALID_STATUS);
        }
        this.career = career;
        this.certification = certification;
        this.status = ExpertStatus.PENDING;
        this.rejectReason = null;
    }

    // 현재 승인된 전문가인지 여부. 1:1 문의(F-30) 개설 시 담당 전문가 자격 확인에 쓰인다.
    public boolean isApproved() {
        return this.status == ExpertStatus.APPROVED;
    }
}