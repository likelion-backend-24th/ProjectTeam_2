package org.example.backend.expert.repository;

import org.example.backend.expert.entity.FeedbackMessageImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackMessageImageRepository extends JpaRepository<FeedbackMessageImage, Long> {
    List<FeedbackMessageImage> findAllByFeedbackMessageIdOrderByImageOrder(Long feedbackMessageId);

    // getMessages()에서 메시지마다 개별 조회하던 것을 배치로 한 번에 가져오기 위해 추가 (N+1 방지)
    List<FeedbackMessageImage> findAllByFeedbackMessageIdInOrderByImageOrder(List<Long> feedbackMessageIds);

    void deleteAllByFeedbackMessageId(Long feedbackMessageId);
}