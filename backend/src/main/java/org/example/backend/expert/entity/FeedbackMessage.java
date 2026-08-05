package org.example.backend.expert.entity;

import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


/**
 * 1:1 문의 스레드 안에서 오가는 개별 메시지.
 * 스레드 개설 시 첫 메시지가 하나 만들어지고, 이후 요청자와 담당 전문가가
 * 번갈아 메시지를 추가하는 구조. feedback_id는 스레드가 삭제되면
 * 함께 삭제.(CASCADE)
 */
@Entity
@Table(name = "feedback_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 메시지가 속한 문의 스레드.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    // 작성자. 질문자(구독자) 또는 담당 전문가 둘 중 하나가 될 수 있음.
    // 어느 쪽인지 검증하는 로직은 FeedbackService.addMessage 체크.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // 메시지 본문.
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    // 메시지 작성 시각.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 스레드 개설 시 첫 메시지, 혹은 이후 대화 메시지를 저장할 때 사용.
    @Builder
    public FeedbackMessage(Feedback feedback, User sender, String content) {
        this.feedback = feedback;
        this.sender = sender;
        this.content = content;
    }
}