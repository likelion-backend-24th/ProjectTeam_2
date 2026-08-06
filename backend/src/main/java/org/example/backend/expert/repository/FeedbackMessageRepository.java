package org.example.backend.expert.repository;

import org.example.backend.expert.entity.FeedbackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessage, Long> {

    // 특정 스레드에 속한 메시지를 작성 시각 오름차순으로 조회.
    List<FeedbackMessage> findByFeedbackIdOrderByCreatedAtAsc(Long feedbackId);
}

// → FeedbackService.getMessages()에서 대화 내용을 시간 순서대로 보여줄 때 사용.
