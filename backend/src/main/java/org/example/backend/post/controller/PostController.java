package org.example.backend.post.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "게시판", description = "취업 정보, 면접 후기, 자소서, 자유 게시글 작성,조회,수정,삭제 API")
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 작성", description = "로그인한 사용자가 카테고리를 선택하여 게시글을 작성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<PostDetailResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostDetailResponse response = postService.createPost(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 등록되었습니다.", response));
    }

    @Operation(summary = "게시글 목록 조회", description = "게시글 목록을 페이징하여 조회합니다. 카테고리로 필터링할 수 있으며, 로그인하지 않아도 조회 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPost(
            @RequestParam(required = false) PostCategory category,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostResponse> posts = postService.getPosts(category, pageable);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록을 조회에 성공했습니다", posts));
    }

    @Operation(summary = "내 게시글 목록 조회", description = "로그인한 사용자 본인이 작성한 게시글 목록을 페이징하여 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostResponse> posts = postService.getMyPosts(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("내 게시글 목록을 조회에 성공했습니다", posts));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 내용과 댓글 목록을 함께 조회합니다. 조회할 때마다 조회수가 1 증가합니다.")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable Long postId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PostDetailResponse detail = postService.getPostDetail(postId, pageable);
        return ResponseEntity.ok(ApiResponse.success("게시글 상세 조회에 성공했습니다", detail));
    }

    @Operation(summary = "게시글 수정", description = "작성자 본인이 게시글의 제목, 내용, 카테고리를 수정합니다.")
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.updatePost(postId, request, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다", null));
    }

    @Operation(summary = "게시글 삭제", description = "작성자 본인 또는 관리자가 게시글을 삭제합니다.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.deletePost(postId, userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다", null));
    }
}
