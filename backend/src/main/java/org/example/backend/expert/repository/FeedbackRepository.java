package org.example.backend.expert.repository;

import org.example.backend.expert.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByExpertProfileId(Long expertProfileId);
    List<Feedback> findByRequesterId(Long requesterId);
    List<Feedback> findByExpertProfileIdAndClosedAtIsNull(Long expertProfileId);

}