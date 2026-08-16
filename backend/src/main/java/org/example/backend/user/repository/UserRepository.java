package org.example.backend.user.repository;

import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = """
        SELECT u FROM User u
        WHERE (:keyword IS NULL OR :keyword = ''
               OR u.nickname LIKE CONCAT('%', :keyword, '%')
               OR u.username LIKE CONCAT('%', :keyword, '%'))
          AND (:role IS NULL OR u.role = :role)
""",
            countQuery = """
        SELECT COUNT(u) FROM User u
        WHERE (:keyword IS NULL OR :keyword = ''
               OR u.nickname LIKE CONCAT('%', :keyword, '%')
               OR u.username LIKE CONCAT('%', :keyword, '%'))
          AND (:role IS NULL OR u.role = :role)
""")
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("role") Role role, Pageable pageable);
}

