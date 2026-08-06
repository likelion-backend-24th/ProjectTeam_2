package org.example.backend.post.repository;

import org.example.backend.post.entity.Post;
import org.example.backend.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post,Long> {

    // category로 필터링 된 게시글을 페이징해서 조회
    Page<Post> findByCategory(PostCategory category, Pageable pageable);

    // 특정 유저가 작성한 게시글을 페이징해서 조회 (마이페이지용)
    Page<Post> findByUserId(Long userId, Pageable pageable);
}
