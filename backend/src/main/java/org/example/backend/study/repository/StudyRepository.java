package org.example.backend.study.repository;

import org.example.backend.study.entity.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {
    @Query(value = """
            SELECT s FROM Study s
            JOIN FETCH s.leader l
            WHERE (:keyword IS NULL OR s.title LIKE CONCAT('%', :keyword, '%'))
            ORDER BY
                CASE WHEN l.subscribed = true
                    AND (s.recruitEnd IS NULL OR s.recruitEnd >= CURRENT_DATE)
                    THEN 0 ELSE 1 END,
                s.createdAt DESC
    """, countQuery = "SELECT COUNT(s) FROM Study s WHERE (:keyword IS NULL OR s.title LIKE CONCAT('%', :keyword, '%'))")
    Page<Study> findAllOrderBySubscribedLeader(@Param("keyword") String keyword, Pageable pageable);
}
