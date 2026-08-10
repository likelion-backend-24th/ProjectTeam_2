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

    @Query("""
    SELECT p FROM Post p
    WHERE (:category IS NULL OR p.category = :category)
      AND (:keyword IS NULL OR :keyword = ''
           OR p.title LIKE CONCAT('%', :keyword, '%')
           OR p.content LIKE CONCAT('%', :keyword, '%'))
    """)
    Page<Post> searchPosts(@Param("category") PostCategory category,
                           @Param("keyword") String keyword,
                           Pageable pageable);

    // 특정 유저가 작성한 게시글을 페이징해서 조회 (마이페이지용)
    Page<Post> findByUserId(Long userId, Pageable pageable);
}
