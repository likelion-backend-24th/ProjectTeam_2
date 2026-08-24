package org.example.backend.expert.repository;

import org.example.backend.expert.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // requesterNickname 표시를 위해 requester를 함께 fetch (N+1 방지 - @ManyToOne이라 페이징에 영향 없음)
    @Query(value = "SELECT f FROM Feedback f JOIN FETCH f.requester WHERE f.expertProfile.id = :expertProfileId",
            countQuery = "SELECT COUNT(f) FROM Feedback f WHERE f.expertProfile.id = :expertProfileId")
    Page<Feedback> findByExpertProfileId(@Param("expertProfileId") Long expertProfileId, Pageable pageable);

    // expertName 표시를 위해 expertProfile과 그 user를 함께 fetch (N+1 방지 - 둘 다 @ManyToOne이라 페이징에 영향 없음)
    @Query(value = "SELECT f FROM Feedback f JOIN FETCH f.expertProfile ep JOIN FETCH ep.user WHERE f.requester.id = :requesterId",
            countQuery = "SELECT COUNT(f) FROM Feedback f WHERE f.requester.id = :requesterId")
    Page<Feedback> findByRequesterId(@Param("requesterId") Long requesterId, Pageable pageable);
    List<Feedback> findByExpertProfileIdAndClosedAtIsNull(Long expertProfileId);
    List<Feedback> findByRequesterIdAndClosedAtIsNull(Long requesterId);
    List<Feedback> findByRequesterIdAndClosedAtIsNull(Long requesterId);
    boolean existsByRequesterIdAndExpertProfileIdAndClosedAtIsNull(Long requesterId, Long expertProfileId);
    long countByRequesterIdAndClosedAtIsNull(Long requesterId);
}