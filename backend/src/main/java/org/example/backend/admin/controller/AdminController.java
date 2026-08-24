package org.example.backend.admin.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.admin.dto.AdminUserResponse;
import org.example.backend.admin.dto.UserStatusUpdateRequest;
import org.example.backend.admin.service.AdminService;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.common.dto.Meta;
import org.example.backend.common.dto.PageMeta;
import org.example.backend.expert.dto.request.ExpertRejectRequest;
import org.example.backend.expert.dto.response.ExpertProfileResponse;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.report.dto.ReportResponse;
import org.example.backend.report.entity.ReportStatus;
import org.example.backend.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.example.backend.expert.dto.response.FeedbackMessageResponse;
import org.example.backend.expert.dto.response.FeedbackResponse;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "관리자", description = "유저 관리, 게시글/댓글/스터디 강제 삭제 등 관리자 전용 API")
public class AdminController {

    private final AdminService adminService;

    //유저 목록 조회
    @Operation(summary = "유저 목록 조회", description = "닉네임/이메일 검색과 권한 필터를 적용해 전체 유저 목록을 페이징하여 조회합니다.")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 10) Pageable pageable){
        Page<AdminUserResponse> page = adminService.getUsers(keyword, role, pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("유저 목록 조회 성공", page.getContent(), meta));
    }


    //유저 상태 변경
    @Operation(summary = "유저 상태 변경", description = "특정 유저의 계정 상태를 ACTIVE 또는 SUSPENDED로 변경합니다.")
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> changeUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request){
        adminService.changeUserStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("유저 상태가 변경되었습니다.", null));
    }

    //게시글 소프트 딜리트 삭제
    @Operation(summary = "게시글 소프트 딜리트 삭제", description = "관리자 권한으로 특정 게시글을 숨김처리합니다.")
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id){
        adminService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("게시글이 숨김처리되었습니다.", null));
    }

    //댓글 소프트 딜리트 삭제
    @Operation(summary = "댓글 소프트 딜리트 삭제", description = "관리자 권한으로 특정 댓글을  숨김처리합니다.")
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id){
        adminService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.success("댓글이 숨김처리되었습니다.", null));
    }

    //스터디 소프트 딜리트 삭제 (연관된 게시글, 댓글,멤버 전부 숨김처리)
    @Operation(summary = "스터디 소프트 딜리트 삭제", description = "관리자 권한으로 특정 스터디를 숨김처리합니다.")
    @DeleteMapping("/studies/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudy(@PathVariable Long id){
        adminService.deleteStudy(id);
        return ResponseEntity.ok(ApiResponse.success("스터디가 숨김처리되었습니다.", null));
    }

    //스터디 게시글 소프트 딜리트 삭제(연관된거 숨김처리)
    @Operation(summary = "스터디 게시글 소프트 딜리트 삭제", description = "관리자 권한으로 특정 스터디 게시글을 숨김처리합니다.")
    @DeleteMapping("/study-posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudyPost(@PathVariable Long id){
        adminService.deleteStudyPost(id);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시글이 숨김처리되었습니다.", null));
    }

    //스터디 게시글 댓글 소프트 딜리트 삭제
    @Operation(summary = "스터디 게시글 댓글 소프트 딜리트 삭제", description = "관리자 권한으로 특정 스터디 게시글 댓글을 숨김처리합니다.")
    @DeleteMapping("/study-post-comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudyPostComment(@PathVariable Long id){
        adminService.deleteStudyPostComment(id);
        return ResponseEntity.ok(ApiResponse.success("스터디 게시글 댓글이 숨김처리되었습니다.", null));
    }

    @Operation(summary = "ADMIN 전문가 목록 조회", description = "status 파라미터로 필터링, 없으면 전체 조회. 페이징 지원.")
    @GetMapping("/experts")
    public ResponseEntity<ApiResponse<List<ExpertProfileResponse>>> getList(
            @RequestParam(required = false) ExpertStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ExpertProfileResponse> page = adminService.getList(status, pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("전문가 목록 조회 성공", page.getContent(), meta));
    }

    @Operation(summary = "ADMIN 전문가 승인", description = "PENDING 상태의 신청을 승인, role이 EXPERT로 전환됩니다.")
    @PatchMapping("/experts/{id}/approve")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> approve(@PathVariable Long id) {
        ExpertProfileResponse response = adminService.approve(id);
        return ResponseEntity.ok(ApiResponse.success("전문가 승인이 완료되었습니다.", response));
    }

    @Operation(summary = "ADMIN 전문가 거절", description = "PENDING 상태의 신청을 거절, 사유를 저장합니다.")
    @PatchMapping("/experts/{id}/reject")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid ExpertRejectRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        ExpertProfileResponse response = adminService.reject(id, reason);
        return ResponseEntity.ok(ApiResponse.success("전문가 신청이 거절되었습니다.", response));
    }

    @Operation(summary = "ADMIN 전문가 자격 박탈", description = "APPROVED 전문가를 박탈, role은 USER로 원복됩니다.")
    @DeleteMapping("/experts/{id}")
    public ResponseEntity<Void> revoke(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid ExpertRejectRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        adminService.revoke(id, reason);
        return ResponseEntity.noContent().build();
    }

    //신고 목록 조회
    @Operation(summary = "신고 목록 조회", description = "신고 목록을 페이징하여 조회합니다. status로 필터링 가능합니다.")
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ReportResponse> page = adminService.getReports(status, pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("신고 목록 조회 성공", page.getContent(), meta));
    }

    //신고 처리 - 콘텐츠 삭제로 종료 (실제 콘텐츠 삭제는 /posts/{id} 등 별도 API 호출 후 이걸 호출)
    @Operation(summary = "신고 처리(삭제)", description = "관리자 권한으로 신고를 콘텐츠 삭제 처리 상태로 변경합니다.")
    @PatchMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveReport(@PathVariable Long id) {
        adminService.resolveReport(id);
        return ResponseEntity.ok(ApiResponse.success("신고가 처리되었습니다.", null));
    }

    //신고 처리 - 콘텐츠는 유지하고 반려
    @Operation(summary = "신고 처리(반려)", description = "관리자 권한으로 신고를 반려 상태로 변경합니다. 콘텐츠는 그대로 유지됩니다.")
    @PatchMapping("/reports/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReport(@PathVariable Long id) {
        adminService.rejectReport(id);
        return ResponseEntity.ok(ApiResponse.success("신고가 반려되었습니다.", null));
    }

    //신고된 피드백 스레드 상세 조회
    @Operation(summary = "ADMIN 피드백 스레드 조회", description = "신고가 접수된 피드백 스레드만 조회 가능합니다. 신고되지 않은 스레드는 403이 반환됩니다.")
    @GetMapping("/feedbacks/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(@PathVariable Long id) {
        FeedbackResponse response = adminService.getFeedback(id);
        return ResponseEntity.ok(ApiResponse.success("문의 스레드 조회 성공", response));
    }

    //신고된 피드백 스레드의 메시지 목록 조회
    @Operation(summary = "ADMIN 피드백 메시지 목록 조회", description = "신고가 접수된 피드백 스레드의 메시지만 조회 가능합니다.")
    @GetMapping("/feedbacks/{id}/messages")
    public ResponseEntity<ApiResponse<List<FeedbackMessageResponse>>> getFeedbackMessages(@PathVariable Long id) {
        List<FeedbackMessageResponse> response = adminService.getFeedbackMessages(id);
        return ResponseEntity.ok(ApiResponse.success("메시지 목록 조회 성공", response));
    }

}
