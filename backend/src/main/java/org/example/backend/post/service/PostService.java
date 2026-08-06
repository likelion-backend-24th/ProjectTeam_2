package org.example.backend.post.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.comment.dto.CommentResponse;
import org.example.backend.comment.entity.Comment;
import org.example.backend.post.entity.Post;
import org.example.backend.post.dto.PostCreateRequest;
import org.example.backend.post.dto.PostDetailResponse;
import org.example.backend.post.dto.PostResponse;
import org.example.backend.post.dto.PostUpdateRequest;
import org.example.backend.post.entity.PostCategory;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.post.exception.PostErrorCode;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.user.entity.Role;
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

        return PostDetailResponse.from(savedPost, List.of());
    }

    public Page<PostResponse> getPosts(PostCategory category, Pageable pageable) {
        Page<Post> posts = (category == null)
                ? postRepository.findAll(pageable)
                : postRepository.findByCategory(category, pageable);
        return posts.map(PostResponse::from);
    }

    public Page<PostResponse> getMyPosts(Long userId, Pageable pageable) {
        Page<Post> posts = postRepository.findByUserId(userId, pageable);
        return posts.map(PostResponse::from);
    }

    public PostDetailResponse getPostDetail(Long postId) {
        // 1. 게시글 조회, 없으면 예외
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        // 2. 댓글 목록 조회
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        // 3. Comment 목록 → CommentResponse 목록으로 변환 및 최종 응답 조립
        return PostDetailResponse.from(post, comments);
    }

    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request, User requester) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        // 권한 체크: 작성자 본인이거나 ADMIN이면 허용 (F-06)
        if (!isOwnerOrAdmin(post, requester)) {
            throw new BusinessException(PostErrorCode.POST_ACCESS_DENIED);
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        // 별도로 저장(save) 안 해도 자동으로 반영돼요 — 이유는 아래 설명
    }

    @Transactional
    public void deletePost(Long postId, User requester) {
        // 1. postId로 게시글 찾기 (없으면 예외)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(PostErrorCode.POST_NOT_FOUND));

        // 2. 작성자 본인이거나 ADMIN이면 허용 (F-06)
        if (!isOwnerOrAdmin(post, requester)) {
            throw new BusinessException(PostErrorCode.POST_ACCESS_DENIED);
        }
        // 3. postRepository.delete(post) 또는 postRepository.deleteById(postId)
        postRepository.delete(post);
    }

    private boolean isOwnerOrAdmin(Post post, User requester) {
        boolean isOwner = post.getUser().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        return isOwner || isAdmin;
    }
}
