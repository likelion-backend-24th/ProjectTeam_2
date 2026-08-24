package org.example.backend.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.common.dto.Meta;
import org.example.backend.common.dto.PageMeta;
import org.example.backend.study.dto.request.StudyRequest;
import org.example.backend.study.dto.request.StudyUpdateRequest;
import org.example.backend.study.dto.response.StudyDetailResponse;
import org.example.backend.study.dto.response.StudyResponse;
import org.example.backend.study.service.StudyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "스터디", description = "스터디 개설, 조회, 수정 , 삭제 API")
public class StudyController {

    private final StudyService studyService;

    @Operation(summary = "스터디 개설", description = "로그인한 사용자가 새 스터디를 개설합니다. 개설자는 자동으로 방장이자 첫 멤버로 등록됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<StudyResponse>> createStudy(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestPart("data") StudyRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images){
        Long userId = user.getUser().getId();
        StudyResponse response = studyService.createStudy(userId, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("스터디 생성", response));
    }

    @Operation(summary = "내가 가입된 스터디 목록 조회", description = "로그인한 사용자가 가입한 스터디 목록을 페이징하여 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<StudyResponse>>> getMyStudies(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = user.getUser().getId();
        Page<StudyResponse> page = studyService.getMyStudies(userId, pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("내가 가입된 스터디 목록 조회", page.getContent(), meta));
    }

    @Operation(summary = "스터디 목록 조회", description = "전체 스터디 목록을 조회합니다. 제목 키워드로 검색할 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudyResponse>>> getAllStudies(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<StudyResponse> page = studyService.getStudies(keyword, pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("스터디 목록 조회", page.getContent(), meta));
    }

    @Operation(summary = "스터디 상세 조회", description = "특정 스터디의 상세 정보와 현재 멤버 수를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudyDetailResponse>> getStudyById(@PathVariable Long id) {
        StudyDetailResponse response = studyService.getStudyById(id);
        return ResponseEntity.ok(ApiResponse.success("스터디 상세 조회", response));
    }

    @Transactional
    @Operation(summary = "스터디 수정", description = "방장 본인이 스터디 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudyResponse>> updateStudy(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody StudyUpdateRequest request) {
        Long userId = user.getUser().getId();
        StudyResponse response = studyService.updateStudy(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("스터디 수정", response));
    }

    @Operation(summary = "스터디 끌올", description = "구독자인 방장이 스터디를 목록 상단으로 끌어올립니다. 24시간에 한 번만 가능하며, 모집이 마감된 스터디는 끌올할 수 없습니다.")
    @PatchMapping("/{id}/bump")
    public ResponseEntity<ApiResponse<StudyResponse>> bumpStudy(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        Long userId = user.getUser().getId();
        StudyResponse response = studyService.bumpStudy(userId, id);
        return ResponseEntity.ok(ApiResponse.success("스터디 끌올 성공", response));
    }

}
