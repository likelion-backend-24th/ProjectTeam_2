package org.example.backend.post.exception;

public class PostAccessDeniedException extends RuntimeException {
    public PostAccessDeniedException(Long postId) {
        super("게시글에 대한 접근 권한이 없습니다. id=" + postId);
    }
}
