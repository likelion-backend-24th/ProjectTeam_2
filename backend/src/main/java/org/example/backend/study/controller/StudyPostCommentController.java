package org.example.backend.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.study.dto.request.StudyPostCommentRequest;
import org.example.backend.study.dto.response.StudyPostCommentResponse;
import org.example.backend.study.dto.request.StudyPostCommentUpdateRequest;
import org.example.backend.study.service.StudyPostCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/studies/{id}/posts/{postId}/comments")
@RequiredArgsConstructor
@Tag(name = "스터디 게시판 댓글", description = "스터디 게시글의 댓글 작성, 수정 ,삭제 API")
public class StudyPostCommentController {

    private final StudyPostCommentService studyPostCommentService;

    @Operation(summary = "스터디 게시판 댓글 작성", description = "해당 스터디 멤버가 게시글에 댓글을 작성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<StudyPostCommentResponse>> createStudyPostComment(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @PathVariable Long postId, @Valid @RequestBody StudyPostCommentRequest request){
        Long userId = user.getUser().getId();
        StudyPostCommentResponse response = studyPostCommentService.createStudyPostComment(userId, id, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("댓글 작성 성공", response));
    }

    @Operation(summary = "스터디 게시판 댓글 수정", description = "댓글 작성자 본인이 댓글 내용을 수정합니다.")
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<StudyPostCommentResponse>> updateStudyPostComment(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @PathVariable Long postId, @PathVariable Long commentId, @Valid @RequestBody StudyPostCommentUpdateRequest request){
        Long userId = user.getUser().getId();
        StudyPostCommentResponse response = studyPostCommentService.updateStudyPostComment(userId, id, postId, commentId, request);
        return ResponseEntity.ok(ApiResponse.success("댓글 수정 성공", response));
    }

    @Operation(summary = "스터디 게시판 댓글 삭제", description = "댓글 작성자 본인이 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudyPostComment(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @PathVariable Long postId, @PathVariable Long commentId){
        Long userId = user.getUser().getId();
        studyPostCommentService.deleteStudyPostComment(userId, id, postId, commentId);
        return ResponseEntity.ok(ApiResponse.success("댓글 삭제 성공", null));
    }
}