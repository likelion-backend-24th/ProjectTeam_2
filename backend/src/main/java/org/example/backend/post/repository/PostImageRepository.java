package org.example.backend.post.repository;

import org.example.backend.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPostIdOrderByImageOrder(Long postId);

    // 목록 조회용: 여러 게시글의 이미지를 한 번에 조회 (N+1 방지). 호출부에서 postId로 그룹핑한다.
    List<PostImage> findAllByPostIdInOrderByPostIdAscImageOrderAsc(List<Long> postIds);

    void deleteAllByPostId(Long postId);
}