package org.example.backend.expert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.expert.dto.ExpertListResponse;
import org.example.backend.expert.dto.ExpertProfileResponse;
import org.example.backend.expert.dto.ExpertRejectRequest;
import org.example.backend.expert.dto.ExpertSignupRequest;
import org.example.backend.expert.dto.ExpertSignupResponse;
import org.example.backend.expert.dto.PublicExpertListResponse;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.expert.service.ExpertProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 전문가 신청/승인/거절/자격박탈/공개조회 API (F-25, F-26, F-27, F-32).
 */
@Tag(name = "전문가", description = "전문가 신청/승인/거절/자격박탈/공개조회 (F-25, F-26, F-27, F-32)")
@RestController
@RequiredArgsConstructor
public class ExpertProfileController {

    private final ExpertProfileService expertProfileService;

    /**
     * F-25: 로그인한 USER가 경력·자격증을 입력해 전문가를 신청한다.
     * Authentication에서 CustomUserDetails를 꺼내 userId를 얻고, 이 값을 그대로
     * ExpertProfileService.signup()에 넘긴다(신규 신청/재신청 분기는 서비스 쪽에서 처리).
     * ExpertSignupRequest에 @NotBlank(career) 등이 걸려있어 @Valid로 요청 바디를 검증한다
     * (실패 시 GlobalExceptionHandler가 400으로 응답).
     * 화면: S-10(전문가 가입/승인 대기 화면).
     */
    @Operation(summary = "F-25 전문가 신청", description = "로그인한 USER가 경력·자격증을 입력해 전문가를 신청합니다.")
    @PostMapping("/api/experts/signup")
    public ResponseEntity<ApiResponse<ExpertSignupResponse>> signup(
            Authentication authentication,
            @RequestBody @Valid ExpertSignupRequest request
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        ExpertSignupResponse response = expertProfileService.signup(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("전문가 신청이 접수되었습니다.", response));
    }

    /**
     * F-32: 승인된(APPROVED) 전문가만 골라 비로그인 사용자를 포함해 전체 공개한다.
     * 별도의 인증/인가 어노테이션이 없어 SecurityConfig의 기본 규칙을 그대로 따르는데,
     * 현재 SecurityConfig는 "/api/auth/**"만 permitAll이고 나머지는 anyRequest().authenticated()라
     * 토큰 없는 요청은 이 엔드포인트에서도 401을 받는다(F-32 명세의 "비로그인 포함 전체 공개"와
     * 다른 부분 - SecurityConfig는 common 영역이라 이 리뷰에서 직접 수정하지는 않음).
     * 화면: S-11(전문가 상담) 상단의 "활동 중인 전문가" 목록.
     */
    @Operation(summary = "F-32 전문가 목록 공개 조회", description = "승인된 전문가를 비로그인 포함 전체 공개합니다.")
    @GetMapping("/api/experts")
    public ResponseEntity<ApiResponse<PublicExpertListResponse>> getPublicList() {
        PublicExpertListResponse response = expertProfileService.getPublicList();
        return ResponseEntity.ok(ApiResponse.success("전문가 목록 조회 성공", response));
    }

    /**
     * F-27: 관리자가 전문가 신청/승인 목록을 조회한다. status 쿼리 파라미터로
     * PENDING/APPROVED/REJECTED 중 하나를 지정해 필터링할 수 있고, 없으면 전체를 반환한다.
     * @PreAuthorize("hasRole('ADMIN')")로 ADMIN이 아니면 403이 나간다.
     * 화면: S-12(어드민-전문가 관리).
     */
    @Operation(summary = "F-27 ADMIN 전문가 목록 조회", description = "status 파라미터로 필터링, 없으면 전체 조회.")
    @GetMapping("/api/admin/experts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpertListResponse>> getList(
            @RequestParam(required = false) ExpertStatus status
    ) {
        ExpertListResponse response = expertProfileService.getList(status);
        return ResponseEntity.ok(ApiResponse.success("전문가 목록 조회 성공", response));
    }

    /**
     * F-26: 관리자가 PENDING 상태의 신청을 승인한다. 승인되면 서비스 내부에서
     * ExpertProfile.status가 APPROVED로, User.role이 EXPERT로 함께 바뀐다.
     * PENDING이 아닌 대상을 승인하려 하면 서비스/엔티티에서 예외(409)가 올라온다.
     */
    @Operation(summary = "F-26 ADMIN 전문가 승인", description = "PENDING 상태의 신청을 승인, role이 EXPERT로 전환됩니다.")
    @PatchMapping("/api/admin/experts/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> approve(@PathVariable Long id) {
        ExpertProfileResponse response = expertProfileService.approve(id);
        return ResponseEntity.ok(ApiResponse.success("전문가 승인이 완료되었습니다.", response));
    }

    /**
     * F-26: 관리자가 PENDING 상태의 신청을 거절한다. body는 필수가 아니며
     * (@RequestBody(required = false)), 없으면 reason은 null로 전달된다.
     * body가 있을 경우에는 @Valid로 ExpertRejectRequest.reason의 @Size(max=255) 검증이 걸린다.
     */
    @Operation(summary = "F-26 ADMIN 전문가 거절", description = "PENDING 상태의 신청을 거절, 사유를 저장합니다.")
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

    /**
     * F-27: 관리자가 이미 승인된 전문가의 자격을 박탈한다. reject()와 마찬가지로
     * body는 선택이고, 있으면 @Valid로 검증된다. 응답 바디 없이 204만 반환한다
     * (다른 API들처럼 ApiResponse로 감싸지 않는다는 점이 이 엔드포인트만의 차이).
     * 서비스 내부에서 ExpertProfile.status는 REJECTED로, User.role은 다시 USER로 되돌아간다.
     */
    @Operation(summary = "F-27 ADMIN 전문가 자격 박탈", description = "APPROVED 전문가를 박탈, role은 USER로 원복됩니다.")
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