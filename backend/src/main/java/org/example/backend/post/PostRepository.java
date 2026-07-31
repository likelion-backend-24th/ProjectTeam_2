package org.example.backend.post;

import org.example.backend.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post,Long> {

    // category로 필터링 된 게시글을 페이징해서 조회
    Page<Post> findbyCategory(String category, Pageable pageable);
}
