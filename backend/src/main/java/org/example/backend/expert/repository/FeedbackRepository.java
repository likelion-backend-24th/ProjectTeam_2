package org.example.backend.expert.repository;

import org.example.backend.expert.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 특정 전문가가 담당하는 문의 스레드 전체를 조회.
    List<Feedback> findByExpertProfileId(Long expertProfileId);

    // 특정 구독자가 개설한 문의 스레드 전체를 조회.
    List<Feedback> findByRequesterId(Long requesterId);
}