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
    // TODO: UserRepository 추가 필요 - User - UserRepository 생성 후 아래 필드 추가
    // private final UserRepository userRepository;

    public Long createPost(PostCreateRequest request, Long userId) {

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());

        // TODO: UserRepository 완성되면 아래 로직 추가
        // User user = userRepository.findById(userId)
        //         .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        // post.setUser(user);


        Post savedPost = postRepository.save(post);
        return savedPost.getId();
    }

    public Page<PostResponse> getPosts(String category, Pageable pageable) {
        Page<Post> posts = postRepository.findByCategory(category, pageable);

        return posts.map(post -> new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getUser().getNickname(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        ));
    }

    public PostDetailResponse getPostDetail(Long postId) {
        // 1. 게시글 조회, 없으면 예외
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 2. 댓글 목록 조회
        List<Comment> comments = commentRepository.findByPostId(postId);

        // 3. Comment 목록 → CommentResponse 목록으로 변환
        List<CommentResponse> commentResponses = comments.stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getContent(),
                        comment.getUser().getNickname(),
                        comment.getCreatedAt()
                ))
                .toList();

        //4. 최종 응답 조립
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getUser().getNickname(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                commentResponses
        );
    }

    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 권한 체크: 요청자가 작성자 본인인지
        if (!post.getUser().getId().equals(userId)) {
            throw new PostAccessDeniedException(postId);
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        // 별도로 저장(save) 안 해도 자동으로 반영돼요 — 이유는 아래 설명
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        // 1. postId로 게시글 찾기 (없으면 예외)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 2. 작성자 본인인지 확인 (아니면 예외)
        if (!post.getUser().getId().equals(userId)) {
            throw new PostAccessDeniedException(postId);
        }
        // 3. postRepository.delete(post) 또는 postRepository.deleteById(postId)
        postRepository.delete(post);

    }
}
