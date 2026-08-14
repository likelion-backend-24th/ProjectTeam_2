package org.example.backend.expert.repository;

import org.example.backend.expert.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByExpertProfileId(Long expertProfileId, Pageable pageable);
    Page<Feedback> findByRequesterId(Long requesterId, Pageable pageable);
    List<Feedback> findByExpertProfileIdAndClosedAtIsNull(Long expertProfileId);
    boolean existsByRequesterIdAndExpertProfileIdAndClosedAtIsNull(Long requesterId, Long expertProfileId);
    long countByRequesterIdAndClosedAtIsNull(Long requesterId);
}