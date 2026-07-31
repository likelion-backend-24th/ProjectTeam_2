package org.example.backend.comment;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.dto.CommentCreateRequest;
import org.example.backend.comment.dto.CommentUpdateRequest;
import org.example.backend.comment.exception.CommentAccessDeniedException;
import org.example.backend.comment.exception.CommentNotFoundException;
import org.example.backend.domain.Comment;
import org.example.backend.domain.Post;
import org.example.backend.post.PostRepository;
import org.example.backend.post.exception.PostNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    // TODO: UserRepository 추가 필요 - User - UserRepository 생성 후 아래 필드 추가
    // private final UserRepository userRepository;

    @Transactional
    public Long createComment(Long postId, CommentCreateRequest request, Long userId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);

        // TODO: UserRepository 완성되면 아래 로직 추가
        // User user = userRepository.findById(userId)
        //         .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        // comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);
        return savedComment.getId();
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
