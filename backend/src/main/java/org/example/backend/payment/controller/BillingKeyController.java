package org.example.backend.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.payment.dto.BillingKeyIssueRequest;
import org.example.backend.payment.dto.BillingKeyPrepareResponse;
import org.example.backend.payment.dto.BillingKeyResponse;
import org.example.backend.payment.service.BillingKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "빌링키", description = "정기결제용 빌링키 발급/선택/삭제 API")
@RestController
@RequestMapping("/api/billing-keys")
@RequiredArgsConstructor
public class BillingKeyController {

    private final BillingKeyService billingKeyService;

    @Operation(summary = "빌링키 발급 준비", description = "카드 등록에 필요한 storeId, channelKey, issueId를 발급합니다.")
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<BillingKeyPrepareResponse>> prepare(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        BillingKeyPrepareResponse response = billingKeyService.prepareBillingKeyIssuance(customUserDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("빌링키 발급 준비가 완료되었습니다.", response));
    }

    @Operation(summary = "빌링키 발급 검증", description = "PortOne 카드 등록 결과를 서버가 재조회하여 검증하고 빌링키를 저장합니다.")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @Valid @RequestBody BillingKeyIssueRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        billingKeyService.verifyAndSaveBillingKey(request.getIssueId(), request.getBillingKey(), customUserDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("빌링키 등록이 완료되었습니다.", null));
    }

    // 내 카드 목록 조회
    @Operation(summary = "내 카드 목록 조회", description = "등록된 카드 목록을 등록 순서대로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BillingKeyResponse>>> getMyBillingKeys(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        List<BillingKeyResponse> response = billingKeyService.getMyBillingKeys(customUserDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("카드 목록 조회 성공", response));
    }

    // 카드 선택 변경
    @Operation(summary = "카드 선택", description = "지정한 카드를 결제/예약에 사용할 카드로 선택합니다. 예약된 자동결제가 있으면 새 카드로 재예약됩니다.")
    @PatchMapping("/{billingKeyId}/select")
    public ResponseEntity<ApiResponse<Void>> selectBillingKey(
            @PathVariable Long billingKeyId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        billingKeyService.selectBillingKey(customUserDetails.getUser().getId(), billingKeyId);
        return ResponseEntity.ok(ApiResponse.success("카드가 선택되었습니다.", null));
    }

    // 카드 삭제
    @Operation(summary = "카드 삭제", description = "등록된 카드를 삭제합니다. 현재 선택된 카드는 삭제할 수 없습니다.")
    @DeleteMapping("/{billingKeyId}")
    public ResponseEntity<ApiResponse<Void>> deleteBillingKey(
            @PathVariable Long billingKeyId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        billingKeyService.deleteBillingKey(customUserDetails.getUser().getId(), billingKeyId);
        return ResponseEntity.ok(ApiResponse.success("카드가 삭제되었습니다.", null));
    }
}