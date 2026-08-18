package org.example.backend.post.repository;

import org.example.backend.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPostIdOrderByImageOrder(Long postId);

    void deleteAllByPostId(Long postId);
}