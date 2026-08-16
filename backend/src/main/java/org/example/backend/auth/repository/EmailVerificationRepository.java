package org.example.backend.auth.repository;

import org.example.backend.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification,Long> {
    // 이메일로 가장 최근 인증 기록 조회   이메일을 계속 보내면 이메일이 계속쌓인는데 제일 최근거로 인증받으려고!!
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    // 이 이메일로, 이 시각 이후에 생서된 기록을 몇 개인지 셀려고..
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);
}
