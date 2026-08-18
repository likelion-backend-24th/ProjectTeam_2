package org.example.backend.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private NotificationTargetType targetType;   // POST or STUDY_POST

    @Column(name = "target_id", nullable = false)
    private Long targetId;    // post.id 또는 studyPost.id

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "comment_preview", length = 50)
    private String commentPreview;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Notification(User receiver, NotificationTargetType targetType, Long targetId, Long commentId, String commentPreview) {
        this.receiver = receiver;
        this.targetType = targetType;
        this.targetId = targetId;
        this.commentId = commentId;
        this.commentPreview = commentPreview;
        this.isRead = false;
    }
}