package org.example.backend.user.repository;

import org.example.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    // 로그인 시 유저 조회
    Optional<User> findByUsername(String username);
    //회원가입 시 중복체크때 사용
    boolean existsByUsername(String username);
    //회원가입 시 닉네임 중복체크때 사용
    boolean existsByNickname(String nickname);
}

