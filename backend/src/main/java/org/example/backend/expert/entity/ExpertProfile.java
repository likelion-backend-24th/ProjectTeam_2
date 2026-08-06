package org.example.backend.expert.entity;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.expert.exception.ExpertErrorCode;
import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    // 소개글. 프로필 상세(F-35)에 노출되는 자기소개, 최대 500자.
    @Column(name = "introduction", length = 500)
    private String introduction;

    // 경력 목록. 신청 시 1건 이상 입력, 다건 등록 가능(F-25).
    @OneToMany(mappedBy = "expertProfile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Career> careers = new ArrayList<>();

    // 자격증 목록. 선택 사항이며 승인 기준으로는 쓰이지 않는다(프로필 노출용, F-25).
    @OneToMany(mappedBy = "expertProfile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications = new ArrayList<>();

    // 심사 상태. PENDING/APPROVED/REJECTED 세 가지 값을 가진다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpertStatus status;

    // 거절/박탈 사유. 승인 시에는 다시 null로 초기화된다.
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    // 신청 시 호출되는 생성자. 상태는 항상 PENDING으로 시작한다.
    // career/certification 목록은 생성 후 addCareer()/addCertification()으로 추가한다.
    @Builder
    public ExpertProfile(User user, String introduction) {
        this.user = user;
        this.introduction = introduction;
        this.status = ExpertStatus.PENDING;
    }

    // 경력 1건 추가.
    public void addCareer(Career career) {
        this.careers.add(career);
    }

    // 자격증 1건 추가.
    public void addCertification(Certification certification) {
        this.certifications.add(certification);
    }

    // 기존 경력·자격증을 모두 비운다. orphanRemoval=true라 DB 행도 함께 삭제된다.
    // 재신청(F-25)이나 수정(F-37)처럼 목록을 통째로 교체할 때 clear 후 다시 add한다.
    public void clearCareersAndCertifications() {
        this.careers.clear();
        this.certifications.clear();
    }

    public void updateIntroduction(String introduction) {
        this.introduction = introduction;
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

    // 반려 상태에서 재신청 시 상태만 되돌린다. 경력/자격증 교체는 서비스 레이어에서
    // clearCareersAndCertifications() + addCareer()/addCertification()으로 처리한다.
    public void reapply() {
        if (this.status != ExpertStatus.REJECTED) {
            throw new BusinessException(ExpertErrorCode.EXPERT_REAPPLY_INVALID_STATUS);
        }
        this.status = ExpertStatus.PENDING;
        this.rejectReason = null;
    }

    // 현재 승인된 전문가인지 여부. 1:1 문의(F-30) 개설 시 담당 전문가 자격 확인에 쓰인다.
    public boolean isApproved() {
        return this.status == ExpertStatus.APPROVED;
    }
}