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
import java.time.LocalDateTime;

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

    @Column(name = "introduction", length = 500)
    private String introduction;

    @OneToMany(mappedBy = "expertProfile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Career> careers = new ArrayList<>();

    @OneToMany(mappedBy = "expertProfile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpertStatus status;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;


    @Builder
    public ExpertProfile(User user, String introduction) {
        this.user = user;
        this.introduction = introduction;
        this.status = ExpertStatus.PENDING;
    }

    public void addCareer(Career career) {
        this.careers.add(career);
    }

    public void addCertification(Certification certification) {
        this.certifications.add(certification);
    }

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
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (this.status != ExpertStatus.PENDING) {
            throw new BusinessException(ExpertErrorCode.EXPERT_REJECT_INVALID_STATUS);
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
        this.approvedAt = null;
    }

    public void revoke(String reason) {
        if (this.status != ExpertStatus.APPROVED) {
            throw new BusinessException(ExpertErrorCode.EXPERT_REVOKE_INVALID_STATUS);
        }
        this.status = ExpertStatus.REJECTED;
        this.rejectReason = reason;
        this.approvedAt = null;

    }

    public void reapply() {
        if (this.status != ExpertStatus.REJECTED) {
            throw new BusinessException(ExpertErrorCode.EXPERT_REAPPLY_INVALID_STATUS);
        }
        this.status = ExpertStatus.PENDING;
        this.rejectReason = null;
    }

    public boolean isApproved() {
        return this.status == ExpertStatus.APPROVED;
    }

    public void updateApplication(String introduction) {
        if (this.status != ExpertStatus.PENDING) {
            throw new BusinessException(ExpertErrorCode.EXPERT_UPDATE_INVALID_STATUS);
        }
        this.introduction = introduction;
    }

}