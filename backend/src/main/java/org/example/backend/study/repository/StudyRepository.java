package org.example.backend.study.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRepository extends JpaRepository<Study, Long> {
    @Query(value = """
            SELECT s FROM Study s
            JOIN FETCH s.leader l
            WHERE (:keyword IS NULL OR s.title LIKE CONCAT('%', :keyword, '%'))
            ORDER BY COALESCE(s.bumpedAt, s.createdAt) DESC
    """, countQuery = "SELECT COUNT(s) FROM Study s WHERE (:keyword IS NULL OR s.title LIKE CONCAT('%', :keyword, '%'))")
    Page<Study> findAllOrderByBumpedAt(@Param("keyword") String keyword, Pageable pageable);

    // 관리자 스터디 목록 조회 - 제목 검색 + 카테고리 필터
    @Query(value = """
            SELECT s FROM Study s
            JOIN FETCH s.leader l
            WHERE (:keyword IS NULL OR s.title LIKE CONCAT('%', :keyword, '%'))
              AND (:category IS NULL OR s.category = :category)
            ORDER BY COALESCE(s.bumpedAt, s.createdAt) DESC
    """, countQuery = """
            SELECT COUNT(s) FROM Study s
            WHERE (:keyword IS NULL OR s.title LIKE CONCAT('%', :keyword, '%'))
              AND (:category IS NULL OR s.category = :category)
    """)
    Page<Study> searchStudiesForAdmin(@Param("keyword") String keyword, @Param("category") StudyCategory category, Pageable pageable);

    // 스터디 방장이 회원탈퇴시 소프트딜리트하기위해 추가한 메서드
    List<Study> findAllByLeaderId(Long leaderId);

    /**
     * 가입 처리(정원 체크 + 저장) 동안 동일 스터디에 대한 동시 가입 요청을 직렬화하기 위한 락.
     * 이 메서드가 트랜잭션에서 해당 행을 "제일 먼저" 읽는 지점이어야 락이 의미가 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Study s WHERE s.id = :id")
    Optional<Study> findByIdForUpdate(@Param("id") Long id);
}
