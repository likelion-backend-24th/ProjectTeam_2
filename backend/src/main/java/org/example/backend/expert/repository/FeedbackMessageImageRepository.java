package org.example.backend.expert.repository;

import org.example.backend.expert.entity.FeedbackMessageImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackMessageImageRepository extends JpaRepository<FeedbackMessageImage, Long> {
    List<FeedbackMessageImage> findAllByFeedbackMessageIdOrderByImageOrder(Long feedbackMessageId);
    void deleteAllByFeedbackMessageId(Long feedbackMessageId);
}