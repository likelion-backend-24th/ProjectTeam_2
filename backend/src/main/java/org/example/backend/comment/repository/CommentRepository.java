package org.example.backend.comment.repository;

import org.example.backend.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);
    // Admin이 게시물에 딸린 댓글 소프트딜리트하기위해 추가
    List<Comment> findAllByPostId(Long postId);
}