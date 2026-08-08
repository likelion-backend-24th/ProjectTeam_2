package org.example.backend.expert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.expert.dto.request.FeedbackCreateRequest;
import org.example.backend.expert.dto.request.FeedbackMessageRequest;
import org.example.backend.expert.dto.response.FeedbackMessageResponse;
import org.example.backend.expert.dto.response.FeedbackResponse;
import org.example.backend.expert.dto.response.MyFeedbackListResponse;
import org.example.backend.expert.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "전문가 피드백", description = "구독자-전문가 1:1 문의 스레드를 개설하고, 구독자와 전문가가 메세지를 주고받는 API")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "전문가 피드백 요청 등록", description = "구독자가 담당 전문가를 지정해 질문 스레드를 개설합니다.")
    @PostMapping("/api/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid FeedbackCreateRequest request
    ) {
        FeedbackResponse response = feedbackService.createFeedback(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문의 스레드가 개설되었습니다.", response));
    }

    @Operation(summary = "피드백 스레드 상세 조회", description = "요청자 또는 담당 전문가만 조회 가능.")
    @GetMapping("/api/feedbacks/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        FeedbackResponse response = feedbackService.getFeedback(userDetails.getUser().getId(), id);
        return ResponseEntity.ok(ApiResponse.success("문의 스레드 조회 성공", response));
    }

    @Operation(summary = "피드백 메시지 등록", description = "메시지를 추가합니다. 담당 전문가 답변 시 ANSWERED 전환.")
    @PostMapping("/api/feedbacks/{id}/messages")
    public ResponseEntity<ApiResponse<FeedbackMessageResponse>> addMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody @Valid FeedbackMessageRequest request
    ) {
        FeedbackMessageResponse response = feedbackService.addMessage(userDetails.getUser().getId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("메시지가 등록되었습니다.", response));
    }

    @Operation(summary = "피드백 메시지 목록 조회", description = "요청자 또는 담당 전문가만 조회 가능.")
    @GetMapping("/api/feedbacks/{id}/messages")
    public ResponseEntity<ApiResponse<List<FeedbackMessageResponse>>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        List<FeedbackMessageResponse> response = feedbackService.getMessages(userDetails.getUser().getId(), id);
        return ResponseEntity.ok(ApiResponse.success("메시지 목록 조회 성공", response));
    }

    @Operation(summary = "내 문의 스레드 목록 조회 (구독자용)", description = "구독자 본인이 개설한 문의 스레드 목록.")
    @GetMapping("/api/feedbacks/me")
    public ResponseEntity<ApiResponse<MyFeedbackListResponse>> getMyFeedbacks(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MyFeedbackListResponse response = feedbackService.getMyFeedbacks(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("내 문의 목록 조회 성공", response));
    }

    @Operation(summary = "받은 문의 목록 조회 (전문가용, 명세서 미확정)", description = "전문가로서 받은 문의 스레드 목록.")
    @GetMapping("/api/feedbacks/expert")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyExpertFeedbacks(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FeedbackResponse> response = feedbackService.getMyExpertFeedbacks(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("받은 문의 목록 조회 성공", response));
    }
}