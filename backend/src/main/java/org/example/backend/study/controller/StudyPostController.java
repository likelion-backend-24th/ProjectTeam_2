package org.example.backend.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.study.dto.request.StudyPostRequest;
import org.example.backend.study.dto.response.StudyPostResponse;
import org.example.backend.study.dto.request.StudyPostUpdateRequest;
import org.example.backend.study.service.StudyPostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies/{id}")
@RequiredArgsConstructor
public class StudyPostController {

    private final StudyPostService studyPostService;

    @Operation(summary = "스터디 게시판(게시글 등록)")
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<StudyPostResponse>> createStudyPosts(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @Valid @RequestBody StudyPostRequest request){
        Long userId = user.getUser().getId();
        StudyPostResponse response = studyPostService.createStudyPost(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("스터디 게시글 등록 성공", response));
    }

    @Operation(summary = "스터디 게시판 목록")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<StudyPostResponse>>> getStudyPosts(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id){
        Long userId = user.getUser().getId();
        List<StudyPostResponse> response = studyPostService.getStudyPosts(userId, id);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시글 목록 조회 성공", response));
    }

    @Operation(summary = "스터디 게시판 상세")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<StudyPostResponse>> getStudyPostDetail(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @PathVariable Long postId){
        Long userId = user.getUser().getId();
        StudyPostResponse response = studyPostService.getStudyPost(userId, id, postId);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시판 상세 조회 성공", response));
    }

    @Operation(summary = "스터디 게시판 수정")
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<StudyPostResponse>> updateStudyPost(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @PathVariable Long postId, @Valid @RequestBody StudyPostUpdateRequest request){
        Long userId = user.getUser().getId();
        StudyPostResponse response = studyPostService.updateStudyPost(userId, id, postId, request);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시글 수정 성공", response));
    }

    @Operation(summary = "스터디 게시판 삭제")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudyPost(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @PathVariable Long postId){
        Long userId = user.getUser().getId();
        studyPostService.deleteStudyPost(userId, id, postId);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시글 삭제 성공", null));
    }
}
