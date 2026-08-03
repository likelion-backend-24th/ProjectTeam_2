package org.example.backend.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.dto.CommentCreateRequest;
import org.example.backend.comment.dto.CommentResponse;
import org.example.backend.comment.dto.CommentUpdateRequest;
import org.example.backend.comment.exception.CommentAccessDeniedException;
import org.example.backend.comment.exception.CommentNotFoundException;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.comment.entity.Comment;
import org.example.backend.post.entity.Post;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.post.exception.PostNotFoundException;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, Long userId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        return CommentResponse.builder()
                .id(savedComment.getId())
                .content(savedComment.getContent())
                .authorNickname(savedComment.getUser().getNickname())
                .createdAt(savedComment.getCreatedAt())
                .build();
    }

    @Transactional
    public void updateComment(Long commentId, CommentUpdateRequest request, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentAccessDeniedException(commentId);
        }

        comment.setContent(request.getContent());
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentAccessDeniedException(commentId);
        }

        commentRepository.delete(comment);
    }
}
