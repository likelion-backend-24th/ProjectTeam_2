package org.example.backend.comment;


import lombok.RequiredArgsConstructor;
import org.example.backend.comment.dto.CommentCreateRequest;
import org.example.backend.comment.dto.CommentUpdateRequest;
import org.example.backend.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createComment(
            @PathVariable Long postId,
            @RequestBody CommentCreateRequest request,
            @RequestParam Long userId
    ) {
        Long commentId = commentService.createComment(postId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 등록되었습니다", commentId));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequest request,
            @RequestParam Long userId
    ) {
        commentService.updateComment(commentId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다", null));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam Long userId
    ) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다", null));
    }
}
