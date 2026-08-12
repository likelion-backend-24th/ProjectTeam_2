package org.example.backend.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.study.dto.request.StudyPostRequest;
import org.example.backend.study.dto.response.StudyPostDetailResponse;
import org.example.backend.study.dto.response.StudyPostResponse;
import org.example.backend.study.dto.request.StudyPostUpdateRequest;
import org.example.backend.study.service.StudyPostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/studies/{id}")
@RequiredArgsConstructor
@Tag(name = "스터디 게시판", description = "스터디 내부 게시글 작성, 조회, 수정, 삭제 API")
public class StudyPostController {

    private final StudyPostService studyPostService;

    @Operation(summary = "스터디 게시판(게시글 등록)", description = "해당 스터디의 멤버가 스터디 내부 게시판에 게시글을 등록합니다.")
    @PostMapping(value ="/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StudyPostResponse>> createStudyPosts(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestPart("data") StudyPostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images){
        Long userId = user.getUser().getId();
        StudyPostResponse response = studyPostService.createStudyPost(userId, id, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("스터디 게시글 등록 성공", response));
    }

    @Operation(summary = "스터디 게시판 목록 조회", description = "해당 스터디 멤버가 스터디 내부 게시글 목록을 조회합니다.")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<StudyPostResponse>>> getStudyPosts(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id){
        Long userId = user.getUser().getId();
        List<StudyPostResponse> response = studyPostService.getStudyPosts(userId, id);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시글 목록 조회 성공", response));
    }

    @Operation(summary = "스터디 게시판 상세 조회", description = "해당 스터디 멤버가 게시글 상세 내용과 댓글 목록을 함께 조회합니다.")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<StudyPostDetailResponse>> getStudyPostDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @PathVariable Long postId){
        Long userId = user.getUser().getId();
        StudyPostDetailResponse response = studyPostService.getStudyPostDetail(userId, id, postId);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시판 상세 조회 성공", response));
    }

    @Operation(summary = "스터디 게시판 수정", description = "게시글 작성자 본인이 제목과 내용을 수정합니다.")
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<StudyPostResponse>> updateStudyPost(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @PathVariable Long postId,
            @Valid @RequestBody StudyPostUpdateRequest request){
        Long userId = user.getUser().getId();
        StudyPostResponse response = studyPostService.updateStudyPost(userId, id, postId, request);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시글 수정 성공", response));
    }

    @Operation(summary = "스터디 게시판 삭제", description = "게시글 작성자 본인이 게시글을 삭제합니다.")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudyPost(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @PathVariable Long postId){
        Long userId = user.getUser().getId();
        studyPostService.deleteStudyPost(userId, id, postId);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시글 삭제 성공", null));
    }
}
