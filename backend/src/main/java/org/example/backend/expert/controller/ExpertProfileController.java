package org.example.backend.expert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.common.dto.Meta;
import org.example.backend.common.dto.PageMeta;
import org.example.backend.expert.dto.request.ExpertSignupRequest;
import org.example.backend.expert.dto.response.ExpertApplicationStatusResponse;
import org.example.backend.expert.dto.response.ExpertProfileDetailResponse;
import org.example.backend.expert.dto.response.ExpertSignupResponse;
import org.example.backend.expert.dto.response.PublicExpertResponse;
import org.example.backend.expert.service.ExpertProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "전문가", description = "전문가 신청, 프로필 조회, 재신청 API")
@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
public class ExpertProfileController {

    private final ExpertProfileService expertProfileService;

    @Operation(summary = "전문가 신청", description = "로그인한 사용자가 경력, 자격증, 소개글을 입력하여 전문가 자격을 신청합니다. 신청 시 상태는 PENDING이 됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<ExpertSignupResponse>> signup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ExpertSignupRequest request
    ) {
        ExpertSignupResponse response = expertProfileService.signup(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("전문가 신청이 접수되었습니다.", response));
    }


    @Operation(summary = "전문가 목록 공개 조회", description = "승인된 전문가를 비로그인 포함 전체 공개합니다. 페이징 지원.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicExpertResponse>>> getPublicList(
            @PageableDefault(size = 12) Pageable pageable
    ) {
        Page<PublicExpertResponse> page = expertProfileService.getPublicList(pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("전문가 목록 조회 성공", page.getContent(), meta));
    }

    @Operation(summary = "전문가 프로필 상세 조회", description = "승인된 전문가의 닉네임, 경력 전체, 자격증, 소개글을 비로그인 포함 전체 공개합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpertProfileDetailResponse>> getDetail(@PathVariable Long id) {
        ExpertProfileDetailResponse response = expertProfileService.getDetail(id);
        return ResponseEntity.ok(ApiResponse.success("전문가 프로필 조회 성공", response));
    }

    @Operation(summary = "전문가 신청 현황 조회", description = "본인의 전문가 신청 상태 및 반려 사유를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ExpertApplicationStatusResponse>> getMyStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ExpertApplicationStatusResponse response = expertProfileService.getMyStatus(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("신청 현황 조회 성공", response));
    }

    @Operation(summary = "전문가 신청서 수정", description = "PENDING 상태의 신청 내용을 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<ExpertSignupResponse>> updateApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ExpertSignupRequest request
    ) {
        ExpertSignupResponse response = expertProfileService.updateApplication(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("전문가 신청서가 수정되었습니다.", response));
    }
}