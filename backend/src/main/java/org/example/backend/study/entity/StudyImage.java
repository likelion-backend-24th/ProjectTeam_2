package org.example.backend.study.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "study_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Study에 @SoftDelete가 붙어있어서 LAZY 참조를 못 씀 (Hibernate 6.4+ 제약)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "image_order", nullable = false)
    private int imageOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StudyImage(Study study, String imageUrl, String originalFileName, int imageOrder) {
        this.study = study;
        this.imageUrl = imageUrl;
        this.originalFileName = originalFileName;
        this.imageOrder = imageOrder;
    }
}