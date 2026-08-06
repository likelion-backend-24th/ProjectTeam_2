package org.example.backend.expert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "career")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Career {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id", nullable = false)
    private ExpertProfile expertProfile;

    @Column(length = 100, nullable = false)
    private String companyName; // 회사명

    @Column(length = 100, nullable = false)
    private String position; // 직함

    @Column(nullable = false)
    private Integer years; // 경력 연차

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private JobField jobField; // 직무 분야

    @Builder
    public Career(ExpertProfile expertProfile, String companyName, String position, Integer years, JobField jobField) {
        this.expertProfile = expertProfile;
        this.companyName = companyName;
        this.position = position;
        this.years = years;
        this.jobField = jobField;
    }
}