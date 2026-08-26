package org.example.backend.post.repository;

import org.example.backend.post.entity.Post;
import org.example.backend.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post,Long> {

    // category로 필터링 된 게시글을 페이징해서 조회
    Page<Post> findByCategory(PostCategory category, Pageable pageable);

    // 작성자를 함께 fetch해 목록 변환 시 작성자 N+1을 방지한다. (user는 @ManyToOne이라 페이징과 함께 써도 row가 늘지 않음)
    @Query(value = """
    SELECT p FROM Post p
    JOIN FETCH p.user
    WHERE (:category IS NULL OR p.category = :category)
      AND (:keyword IS NULL OR :keyword = ''
           OR p.title LIKE CONCAT('%', :keyword, '%')
           OR p.content LIKE CONCAT('%', :keyword, '%'))
    """,
    countQuery = """
    SELECT COUNT(p) FROM Post p
    WHERE (:category IS NULL OR p.category = :category)
      AND (:keyword IS NULL OR :keyword = ''
           OR p.title LIKE CONCAT('%', :keyword, '%')
           OR p.content LIKE CONCAT('%', :keyword, '%'))
    """)
    Page<Post> searchPosts(@Param("category") PostCategory category,
                           @Param("keyword") String keyword,
                           Pageable pageable);

    // 특정 유저가 작성한 게시글을 페이징해서 조회 (마이페이지용). 작성자를 함께 fetch해 N+1 방지
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user WHERE p.user.id = :userId",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
    Page<Post> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
