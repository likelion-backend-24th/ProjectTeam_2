package org.example.backend.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.common.dto.Meta;
import org.example.backend.common.dto.PageMeta;
import org.example.backend.study.dto.*;
import org.example.backend.study.service.StudyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    // 현재 AuthenticationPrincipal 사용이 불가하므로 하드코딩
    private static final Long USER_ID = 1L;

    @Operation(summary = "스터디 개설")
    @PostMapping
    public ResponseEntity<ApiResponse<StudyResponse>> createStudy(@Valid @RequestBody StudyRequest request){
        StudyResponse response = studyService.createStudy(USER_ID, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("스터디 생성", response));
    }

    @Operation(summary = "스터디 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudyResponse>>> getAllStudies(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<StudyResponse> page = studyService.getStudies(keyword, pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("스터디 목록 조회", page.getContent(), meta));
    }

    @Operation(summary = "스터디 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudyDetailResponse>> getStudyById(@PathVariable Long id) {
        StudyDetailResponse response = studyService.getStudyById(id);
        return ResponseEntity.ok(ApiResponse.success("스터디 상세 조회", response));
    }

    @Operation(summary = "스터디 수정")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudyResponse>> updateStudy(
            @PathVariable Long id,
            @Valid @RequestBody StudyUpdateRequest request) {
        StudyResponse response = studyService.updateStudy(USER_ID, id, request);
        return ResponseEntity.ok(ApiResponse.success("스터디 수정", response));
    }

    @Operation(summary = "스터디 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudy(@PathVariable Long id) {
        studyService.deleteStudy(USER_ID, id);
        return ResponseEntity.ok(ApiResponse.success("스터디 삭제", null));
    }

    @Operation(summary = "스터디 가입 신청")
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<StudyMemberResponse>> joinStudy(@PathVariable Long id) {
        StudyMemberResponse response = studyService.joinStudy(USER_ID, id);
        return ResponseEntity.ok(ApiResponse.success("스터디 가입", response));
    }
}
