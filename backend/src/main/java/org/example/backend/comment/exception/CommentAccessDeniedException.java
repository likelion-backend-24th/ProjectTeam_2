package org.example.backend.comment.exception;

public class CommentAccessDeniedException extends RuntimeException {
    public CommentAccessDeniedException(Long commentId) {
        super("댓글 수정 권한이 없습니다. id=" + commentId);
    }
}