package org.example.backend.expert.repository;

import org.example.backend.expert.entity.FeedbackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessage, Long> {

    List<FeedbackMessage> findByFeedbackIdOrderByCreatedAtAsc(Long feedbackId);
}