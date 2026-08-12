package org.example.backend.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.report.dto.ReportCreateRequest;
import org.example.backend.report.dto.ReportResponse;
import org.example.backend.report.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "신고", description = "게시글·댓글·스터디게시글·스터디댓글 공용 신고 API")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 등록", description = "게시글, 댓글, 스터디게시글, 스터디댓글을 신고합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ReportResponse response = reportService.createReport(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("신고가 접수되었습니다.", response));
    }
}