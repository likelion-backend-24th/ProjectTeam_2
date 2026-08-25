package org.example.backend.expert.entity;

import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Feedback에 @SoftDelete가 붙어있어서 LAZY 참조를 못 씀 (Hibernate 6.4+ 제약)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public FeedbackMessage(Feedback feedback, User sender, String content) {
        this.feedback = feedback;
        this.sender = sender;
        this.content = content;
    }
}