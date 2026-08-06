package org.example.backend.expert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전문가 신청자의 자격증 1건. 선택 사항이며 승인 기준에는 반영되지 않는다(프로필 노출용).
 * 하나의 ExpertProfile은 여러 개의 Certification을 가질 수 있다(1:N).
 */
@Entity
@Table(name = "certification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id", nullable = false)
    private ExpertProfile expertProfile;

    @Column(length = 100, nullable = false)
    private String name; // 자격증명

    @Column(length = 100, nullable = false)
    private String issuer; // 발급 기관

    @Column(nullable = false)
    private Integer acquiredYear; // 취득 연도

    @Builder
    public Certification(ExpertProfile expertProfile, String name, String issuer, Integer acquiredYear) {
        this.expertProfile = expertProfile;
        this.name = name;
        this.issuer = issuer;
        this.acquiredYear = acquiredYear;
    }
}