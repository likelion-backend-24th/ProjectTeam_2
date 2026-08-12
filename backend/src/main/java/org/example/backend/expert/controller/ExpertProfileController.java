package org.example.backend.expert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.expert.dto.response.*;
import org.example.backend.expert.dto.request.ExpertRejectRequest;
import org.example.backend.expert.dto.request.ExpertSignupRequest;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.expert.service.ExpertProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@Tag(name = "전문가", description = "전문가 신청, 프로필 조회, 재신청 및 관리자 승인/거절/자격 박탈 API")
@RestController
@RequiredArgsConstructor
public class ExpertProfileController {

    private final ExpertProfileService expertProfileService;

    @Operation(summary = "전문가 신청", description = "로그인한 사용자가 경력, 자격증, 소개글을 입력하여 전문가 자격을 신청합니다. 신청 시 상태는 PENDING이 됩니다.")
    @PostMapping("/api/experts/signup")
    public ResponseEntity<ApiResponse<ExpertSignupResponse>> signup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ExpertSignupRequest request
    ) {
        ExpertSignupResponse response = expertProfileService.signup(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("전문가 신청이 접수되었습니다.", response));
    }


    @Operation(summary = "전문가 목록 공개 조회", description = "승인된 전문가를 비로그인 포함 전체 공개합니다.")
    @GetMapping("/api/experts")
    public ResponseEntity<ApiResponse<PublicExpertListResponse>> getPublicList() {
        PublicExpertListResponse response = expertProfileService.getPublicList();
        return ResponseEntity.ok(ApiResponse.success("전문가 목록 조회 성공", response));
    }

    @Operation(summary = "전문가 프로필 상세 조회", description = "승인된 전문가의 닉네임, 경력 전체, 자격증, 소개글을 비로그인 포함 전체 공개합니다.")
    @GetMapping("/api/experts/{id}")
    public ResponseEntity<ApiResponse<ExpertProfileDetailResponse>> getDetail(@PathVariable Long id) {
        ExpertProfileDetailResponse response = expertProfileService.getDetail(id);
        return ResponseEntity.ok(ApiResponse.success("전문가 프로필 조회 성공", response));
    }

    @Operation(summary = "전문가 신청 현황 조회", description = "본인의 전문가 신청 상태 및 반려 사유를 조회합니다.")
    @GetMapping("/api/experts/me")
    public ResponseEntity<ApiResponse<ExpertApplicationStatusResponse>> getMyStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ExpertApplicationStatusResponse response = expertProfileService.getMyStatus(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("신청 현황 조회 성공", response));
    }

    @Operation(summary = "전문가 신청서 수정", description = "PENDING 상태의 신청 내용을 수정합니다.")
    @PatchMapping("/api/experts/me")
    public ResponseEntity<ApiResponse<ExpertSignupResponse>> updateApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ExpertSignupRequest request
    ) {
        ExpertSignupResponse response = expertProfileService.updateApplication(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("전문가 신청서가 수정되었습니다.", response));
    }


    @Operation(summary = "ADMIN 전문가 목록 조회", description = "status 파라미터로 필터링, 없으면 전체 조회.")
    @GetMapping("/api/admin/experts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpertListResponse>> getList(
            @RequestParam(required = false) ExpertStatus status
    ) {
        ExpertListResponse response = expertProfileService.getList(status);
        return ResponseEntity.ok(ApiResponse.success("전문가 목록 조회 성공", response));
    }

    @Operation(summary = "ADMIN 전문가 승인", description = "PENDING 상태의 신청을 승인, role이 EXPERT로 전환됩니다.")
    @PatchMapping("/api/admin/experts/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> approve(@PathVariable Long id) {
        ExpertProfileResponse response = expertProfileService.approve(id);
        return ResponseEntity.ok(ApiResponse.success("전문가 승인이 완료되었습니다.", response));
    }

    @Operation(summary = "ADMIN 전문가 거절", description = "PENDING 상태의 신청을 거절, 사유를 저장합니다.")
    @PatchMapping("/api/admin/experts/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid ExpertRejectRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        ExpertProfileResponse response = expertProfileService.reject(id, reason);
        return ResponseEntity.ok(ApiResponse.success("전문가 신청이 거절되었습니다.", response));
    }

    @Operation(summary = "ADMIN 전문가 자격 박탈", description = "APPROVED 전문가를 박탈, role은 USER로 원복됩니다.")
    @DeleteMapping("/api/admin/experts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revoke(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid ExpertRejectRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        expertProfileService.revoke(id, reason);
        return ResponseEntity.noContent().build();
    }
}