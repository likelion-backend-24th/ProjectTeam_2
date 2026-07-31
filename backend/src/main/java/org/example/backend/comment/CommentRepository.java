package org.example.backend.comment;

import org.example.backend.domain.Comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, List> {

    List<Comment> findByPost(Long postId);
}
