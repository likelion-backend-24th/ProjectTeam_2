package org.example.backend.auth.repository;

import org.example.backend.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification,Long> {
    // 이메일로 가장 최근 인증 기록 조회
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);
}
