package org.example.backend.expert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.expert.dto.FeedbackCreateRequest;
import org.example.backend.expert.dto.FeedbackMessageRequest;
import org.example.backend.expert.dto.FeedbackMessageResponse;
import org.example.backend.expert.dto.FeedbackResponse;
import org.example.backend.expert.dto.MyFeedbackListResponse;
import org.example.backend.expert.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "전문가 피드백", description = "구독자-전문가 1:1 문의 스레드 (F-30)")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "F-30 전문가 피드백 요청 등록", description = "구독자가 담당 전문가를 지정해 질문 스레드를 개설합니다.")
    @PostMapping("/api/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            Authentication authentication,
            @RequestBody FeedbackCreateRequest request
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        FeedbackResponse response = feedbackService.createFeedback(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문의 스레드가 개설되었습니다.", response));
    }

    @Operation(summary = "F-30 피드백 스레드 상세 조회", description = "스레드 상태와 기본 정보를 조회합니다.")
    @GetMapping("/api/feedbacks/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(@PathVariable Long id) {
        FeedbackResponse response = feedbackService.getFeedback(id);
        return ResponseEntity.ok(ApiResponse.success("문의 스레드 조회 성공", response));
    }

    @Operation(summary = "F-30 피드백 메시지 등록", description = "메시지를 추가합니다. 담당 전문가 답변 시 ANSWERED 전환.")
    @PostMapping("/api/feedbacks/{id}/messages")
    public ResponseEntity<ApiResponse<FeedbackMessageResponse>> addMessage(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody FeedbackMessageRequest request
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        FeedbackMessageResponse response = feedbackService.addMessage(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("메시지가 등록되었습니다.", response));
    }

    @Operation(summary = "F-30 피드백 메시지 목록 조회", description = "메시지를 작성 시각 오름차순으로 조회합니다.")
    @GetMapping("/api/feedbacks/{id}/messages")
    public ResponseEntity<ApiResponse<List<FeedbackMessageResponse>>> getMessages(@PathVariable Long id) {
        List<FeedbackMessageResponse> response = feedbackService.getMessages(id);
        return ResponseEntity.ok(ApiResponse.success("메시지 목록 조회 성공", response));
    }

    @Operation(summary = "F-30 내 문의 스레드 목록 조회 (구독자용)", description = "구독자 본인이 개설한 문의 스레드 목록.")
    @GetMapping("/api/feedbacks/me")
    public ResponseEntity<ApiResponse<MyFeedbackListResponse>> getMyFeedbacks(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        MyFeedbackListResponse response = feedbackService.getMyFeedbacks(userId);
        return ResponseEntity.ok(ApiResponse.success("내 문의 목록 조회 성공", response));
    }

    // 참고: 명세서 F-30 "관련 API" 목록엔 없는 엔드포인트입니다. 팀 확인 필요.
    @Operation(summary = "받은 문의 목록 조회 (전문가용, 명세서 미확정)", description = "전문가로서 받은 문의 스레드 목록.")
    @GetMapping("/api/feedbacks/expert")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyExpertFeedbacks(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        List<FeedbackResponse> response = feedbackService.getMyExpertFeedbacks(userId);
        return ResponseEntity.ok(ApiResponse.success("받은 문의 목록 조회 성공", response));
    }
}