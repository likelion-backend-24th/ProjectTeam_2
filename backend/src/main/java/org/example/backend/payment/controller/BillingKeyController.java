package org.example.backend.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.payment.dto.request.BillingKeyCompleteRequest;
import org.example.backend.payment.dto.response.BillingKeyPrepareResponse;
import org.example.backend.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "카드(빌링키)", description = "정기결제용 카드 등록 API")
@RestController
@RequestMapping("/api/billing-keys")
@RequiredArgsConstructor
public class BillingKeyController {

    private final PaymentService paymentService;

    @Operation(summary = "카드 등록 준비", description = "PortOne 카드 등록 팝업 호출에 필요한 값을 발급한다.")
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<BillingKeyPrepareResponse>> prepare(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BillingKeyPrepareResponse response = paymentService.prepareBillingKeyIssue(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("카드 등록 준비가 완료되었습니다.", response));
    }

    @Operation(summary = "카드 등록 완료", description = "PortOne이 발급한 빌링키를 서버가 재조회로 검증한 뒤 저장한다.")
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Void>> complete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BillingKeyCompleteRequest request
    ) {
        paymentService.completeBillingKeyIssue(userDetails.getUser().getId(), request.billingKey(), request.billingIssueToken());
        return ResponseEntity.ok(ApiResponse.success("카드가 등록되었습니다.", null));
    }
}
