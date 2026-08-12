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
@Table(name = "study_post_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_post_id", nullable = false)
    private StudyPost studyPost;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "image_order", nullable = false)
    private int imageOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StudyPostImage(StudyPost studyPost, String imageUrl, String originalFileName, int imageOrder) {
        this.studyPost = studyPost;
        this.imageUrl = imageUrl;
        this.originalFileName = originalFileName;
        this.imageOrder = imageOrder;
    }
}