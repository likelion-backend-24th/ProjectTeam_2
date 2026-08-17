package org.example.backend.post.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.entity.Comment;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.post.dto.PostCreateRequest;
import org.example.backend.post.dto.PostDetailResponse;
import org.example.backend.post.dto.PostResponse;
import org.example.backend.post.dto.PostUpdateRequest;
import org.example.backend.post.entity.Post;
import org.example.backend.post.entity.PostCategory;
import org.example.backend.post.exception.PostErrorCode;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;


    public PostDetailResponse createPost(PostCreateRequest request, User user) {
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        post.setUser(user);

        Post savedPost = postRepository.save(post);

        return PostDetailResponse.from(savedPost, Page.empty());
    }

    public Page<PostResponse> getPosts(PostCategory category, String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.searchPosts(category, keyword, pageable);
        return posts.map(PostResponse::from);
    }

    public Page<PostResponse> getMyPosts(Long userId, Pageable pageable) {
        Page<Post> posts = postRepository.findByUserId(userId, pageable);
        return posts.map(PostResponse::from);
    }

    @Transactional
    public PostDetailResponse getPostDetail(Long postId, Pageable pageable) {
        // 1. 게시글 조회, 없으면 예외
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        // 2. 조회수 1 증가
        post.setViewCount(post.getViewCount() + 1);

        // 3. 댓글 목록 페이징 조회
        Page<Comment> comments = commentRepository.findByPostId(postId, pageable);

        // 4. Comment 목록 → CommentResponse 목록으로 변환 및 최종 응답 조립
        return PostDetailResponse.from(post, comments);
    }

    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request, User requester) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        // 권한 체크: 작성자 본인만 허용 (관리자는 /api/admin/** 전용 강제 경로를 씀)
        if (!isOwner(post, requester)) {
            throw new BusinessException(PostErrorCode.POST_ACCESS_DENIED);
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        // 별도로 저장(save) 안 해도 자동으로 반영돼요 — 이유는 아래 설명
    }

    @Transactional
    public void deletePost(Long postId, User requester) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        // 권한 체크: 작성자 본인만 허용 (관리자는 /api/admin/** 전용 강제 경로를 씀)
        if (!isOwner(post, requester)) {
            throw new BusinessException(PostErrorCode.POST_ACCESS_DENIED);
        }

        deletePostCascade(post);   // ← 원래 postRepository.delete(post) 한 줄이던 걸 공통 메서드 호출로 교체
    }

    // 관리자 강제 삭제 전용. 권한 체크 없음 — /api/admin/** 경로에서 이미 ADMIN만 통과되기 때문
    @Transactional
    public void adminDeletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));
        deletePostCascade(post);
    }

    // 실제 삭제 로직(게시글 + 딸린 댓글). deletePost/adminDeletePost 둘 다 이걸 씀
    private void deletePostCascade(Post post) {
        List<Comment> comments = commentRepository.findAllByPostId(post.getId());
        commentRepository.deleteAll(comments);
        postRepository.delete(post);
    }

    private boolean isOwner(Post post, User requester) {
        return post.getUser().getId().equals(requester.getId());
    }
}
