package org.example.backend.expert.repository;

import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 전문가 신청, 심사 정보 조회용 Repository.
 */
public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, Long> {

    // 유저 1명당 프로필이 1개, 단건 조회.
    Optional<ExpertProfile> findByUserId(Long userId);

    // 유저의 프로필 존재 여부만 확인하는 메서드.
    boolean existsByUserId(Long userId);

    // status로 필터링한 목록 조회.
    // ADMIN 전문가 목록(status 파라미터로 필터링)
    // 공개 전문가 목록(APPROVED만 조회)
    List<ExpertProfile> findByStatus(ExpertStatus status);
}