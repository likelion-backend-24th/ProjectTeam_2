package org.example.backend.post;


import lombok.RequiredArgsConstructor;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.post.dto.PostResponse;
import org.example.backend.post.dto.PostUpdateRequest;
import org.springframework.data.domain.Page;
import org.example.backend.post.dto.PostCreateRequest;
import org.example.backend.post.dto.PostDetailResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createPost(
            @RequestBody PostCreateRequest request,
            @RequestParam Long userId // TODO: 인증 완성되면 @AuthenticationPrincipal로 교체
    ) {
        Long postId = postService.createPost(request, userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 등록되었습니다", postId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPost(
            @RequestParam(required = false) String category,
            Pageable pageable
    ){
        Page<PostResponse> posts = postService.getPosts(category, pageable);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록을 조회에 성공했습니다", posts));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable Long postId
    ){
        PostDetailResponse detail = postService.getPostDetail(postId);
        return ResponseEntity.ok(ApiResponse.success("게시글 상세 조회를 성공했습니다", detail));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request,
            @RequestParam Long userId
    ) {
        postService.updatePost(postId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다", null));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @RequestParam Long userId
    ) {
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다", null));
    }
}
