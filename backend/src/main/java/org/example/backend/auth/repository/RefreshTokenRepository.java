package org.example.backend.auth.repository;

import org.example.backend.auth.entity.RefreshToken;
import org.example.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // 유저의 RefreshToken 조회(DB저장용)
    Optional<RefreshToken> findByUser(User user);
    // DB에 저장된 토큰 조회
    Optional<RefreshToken> findByToken(String token);
    // 로그아웃시 DB에서 토큰 삭제
    void deleteByUser(User user);
}