package org.example.backend.expert.repository;

import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, Long> {

    Optional<ExpertProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId); // 참고: F-25 재신청 로직 이후로 안 씀. 지워도 무방.

    List<ExpertProfile> findByStatus(ExpertStatus status);
}