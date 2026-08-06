package org.example.backend.post.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.post.entity.PostCategory;
import org.example.backend.post.service.PostService;
import org.example.backend.post.dto.PostResponse;
import org.example.backend.post.dto.PostUpdateRequest;
import org.springframework.data.domain.Page;
import org.example.backend.post.dto.PostCreateRequest;
import org.example.backend.post.dto.PostDetailResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostDetailResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostDetailResponse response = postService.createPost(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 등록되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPost(
            @RequestParam(required = false) PostCategory category,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostResponse> posts = postService.getPosts(category, pageable);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록을 조회에 성공했습니다", posts));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostResponse> posts = postService.getMyPosts(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("내 게시글 목록을 조회에 성공했습니다", posts));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable Long postId
    ) {
        PostDetailResponse detail = postService.getPostDetail(postId);
        return ResponseEntity.ok(ApiResponse.success("게시글 상세 조회에 성공했습니다", detail));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.updatePost(postId, request, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다", null));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.deletePost(postId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다", null));
    }
}
