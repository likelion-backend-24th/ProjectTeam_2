package org.example.backend.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.payment.dto.BillingKeyIssueRequest;
import org.example.backend.payment.dto.BillingKeyPrepareResponse;
import org.example.backend.payment.service.BillingKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "빌링키", description = "정기결제용 빌링키 발급 API")
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

    // 빌링키 발급 검증
    @Operation(summary = "빌링키 발급 검증", description = "PortOne 카드 등록 결과를 서버가 재조회하여 검증하고 빌링키를 저장합니다.")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @Valid @RequestBody BillingKeyIssueRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        billingKeyService.verifyAndSaveBillingKey(request.getIssueId(), request.getBillingKey(), customUserDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("빌링키 등록이 완료되었습니다.", null));
    }
}