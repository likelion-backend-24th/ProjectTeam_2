package org.example.backend.expert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_message_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackMessageImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_message_id", nullable = false)
    private FeedbackMessage feedbackMessage;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "image_order", nullable = false)
    private int imageOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FeedbackMessageImage(FeedbackMessage feedbackMessage, String imageUrl, String originalFileName, int imageOrder) {
        this.feedbackMessage = feedbackMessage;
        this.imageUrl = imageUrl;
        this.originalFileName = originalFileName;
        this.imageOrder = imageOrder;
    }
}