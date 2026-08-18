package org.example.backend.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.dto.CommentCreateRequest;
import org.example.backend.comment.dto.CommentResponse;
import org.example.backend.comment.dto.CommentUpdateRequest;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.comment.exception.CommentErrorCode;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.comment.entity.Comment;
import org.example.backend.notification.entity.NotificationTargetType;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.post.entity.Post;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.post.exception.PostErrorCode;
import org.example.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final NotificationService notificationService;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, User user) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        notificationService.notifyComment(
                savedComment.getPost().getUser(),
                user,
                NotificationTargetType.POST,
                savedComment.getPost().getId(),
                savedComment.getId(),
                request.getContent()
        );

        return CommentResponse.from(savedComment);
    }

    @Transactional
    public void updateComment(Long postId, Long commentId, CommentUpdateRequest request, User requester) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

        // 이 댓글이 진짜 그 게시글에 속한 댓글인지 확인
        if (!comment.getPost().getId().equals(postId)) {
            throw new BusinessException(CommentErrorCode.COMMENT_POST_MISMATCH);
        }

        // 권한 체크: 작성자 본인만 허용 (관리자는 /api/admin/** 전용 강제 경로를 씀)
        if (!isOwner(comment, requester)) {
            throw new BusinessException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }

        comment.setContent(request.getContent());
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, User requester) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));

        // 이 댓글이 진짜 그 게시글에 속한 댓글인지 확인
        if (!comment.getPost().getId().equals(postId)) {
            throw new BusinessException(CommentErrorCode.COMMENT_POST_MISMATCH);
        }

        // 권한 체크: 작성자 본인만 허용 (관리자는 /api/admin/** 전용 강제 경로를 씀)
        if (!isOwner(comment, requester)) {
            throw new BusinessException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }

        commentRepository.delete(comment);
    }

    // 관리자 강제 삭제 전용. postId도 필요 없고 권한 체크도 없음
    @Transactional
    public void adminDeleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
        commentRepository.delete(comment);
    }

    private boolean isOwner(Comment comment, User requester) {
        return comment.getUser().getId().equals(requester.getId());
    }
}
