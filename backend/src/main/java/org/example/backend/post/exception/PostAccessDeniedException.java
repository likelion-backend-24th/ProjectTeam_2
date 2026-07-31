package org.example.backend.post.exception;

public class PostAccessDeniedException extends RuntimeException {
    public PostAccessDeniedException(Long postId) {
        super("게시글에 대한 접근이 거부되었습니다. id=" + postId);
    }
}
