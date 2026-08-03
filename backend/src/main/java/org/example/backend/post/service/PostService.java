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
import org.example.backend.post.exception.PostAccessDeniedException;
import org.example.backend.post.exception.PostNotFoundException;
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

        return PostDetailResponse.builder()
                .id(savedPost.getId())
                .title(savedPost.getTitle())
                .content(savedPost.getContent())
                .category(savedPost.getCategory())
                .authorNickname(savedPost.getUser().getNickname())
                .createdAt(savedPost.getCreatedAt())
                .updatedAt(savedPost.getUpdatedAt())
                .build();
    }

    public Page<PostResponse> getPosts(String category, Pageable pageable) {
        Page<Post> posts = (category == null || category.isBlank())
                ? postRepository.findAll(pageable)
                : postRepository.findByCategory(category, pageable);

        return posts.map(post -> PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build());
    }

    public PostDetailResponse getPostDetail(Long postId) {
        // 1. 게시글 조회, 없으면 예외
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 2. 댓글 목록 조회
        List<Comment> comments = commentRepository.findByPostId(postId);

        // 3. Comment 목록 → CommentResponse 목록으로 변환
        List<CommentResponse> commentResponses = comments.stream()
                .map(comment -> CommentResponse.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .authorNickname(comment.getUser().getNickname())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .toList();

        //4. 최종 응답 조립
        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .authorNickname(post.getUser().getNickname())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(commentResponses)
                .build();
    }

    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request, User requester) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 권한 체크: 작성자 본인이거나 ADMIN이면 허용 (F-06)
        if (!isOwnerOrAdmin(post, requester)) {
            throw new PostAccessDeniedException(postId);
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
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 2. 작성자 본인이거나 ADMIN이면 허용 (F-06)
        if (!isOwnerOrAdmin(post, requester)) {
            throw new PostAccessDeniedException(postId);
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
