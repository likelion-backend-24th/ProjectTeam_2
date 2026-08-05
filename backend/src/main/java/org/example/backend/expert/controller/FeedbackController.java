package org.example.backend.expert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/**
 * 구독자-전문가 1:1 문의 스레드 API (F-30). 화면: S-11(전문가 상담).
 */
@Tag(name = "전문가 피드백", description = "구독자-전문가 1:1 문의 스레드 (F-30)")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * F-30: 구독자가 담당 전문가를 지정해 질문 스레드를 개설한다(스레드 + 첫 메시지 생성, 201).
     * FeedbackCreateRequest에 @NotNull(expertProfileId), @NotBlank(content)가 걸려있어
     * @Valid로 요청 바디를 먼저 검증하고, 구독 여부·전문가 승인 여부 등은
     * FeedbackService.createFeedback()에서 확인한다.
     */
    @Operation(summary = "F-30 전문가 피드백 요청 등록", description = "구독자가 담당 전문가를 지정해 질문 스레드를 개설합니다.")
    @PostMapping("/api/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            Authentication authentication,
            @RequestBody @Valid FeedbackCreateRequest request
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        FeedbackResponse response = feedbackService.createFeedback(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문의 스레드가 개설되었습니다.", response));
    }

    /**
     * F-30: 스레드 상세(상태, 참여자 id, 생성/답변 시각)를 조회한다.
     * Authentication 파라미터가 없어 로그인 여부와 무관하게(단, SecurityConfig 기본 규칙상
     * 유효한 토큰은 있어야 함) id만 알면 조회가 되고, 호출자가 이 스레드의 요청자인지
     * 담당 전문가인지 구분해서 막는 로직은 이 엔드포인트에는 없다.
     */
    @Operation(summary = "F-30 피드백 스레드 상세 조회", description = "스레드 상태와 기본 정보를 조회합니다.")
    @GetMapping("/api/feedbacks/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(@PathVariable Long id) {
        FeedbackResponse response = feedbackService.getFeedback(id);
        return ResponseEntity.ok(ApiResponse.success("문의 스레드 조회 성공", response));
    }

    /**
     * F-30: 스레드에 메시지를 추가한다(201). FeedbackMessageRequest.content에 @NotBlank가
     * 걸려있어 @Valid로 빈 메시지를 막고, 요청자/담당 전문가 여부 확인과 담당 전문가가
     * 답변했을 때 스레드를 ANSWERED로 전환하는 처리는 FeedbackService.addMessage()에서 한다.
     */
    @Operation(summary = "F-30 피드백 메시지 등록", description = "메시지를 추가합니다. 담당 전문가 답변 시 ANSWERED 전환.")
    @PostMapping("/api/feedbacks/{id}/messages")
    public ResponseEntity<ApiResponse<FeedbackMessageResponse>> addMessage(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody @Valid FeedbackMessageRequest request
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        FeedbackMessageResponse response = feedbackService.addMessage(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("메시지가 등록되었습니다.", response));
    }

    /**
     * F-30: 스레드의 메시지 목록을 작성 시각 오름차순으로 조회한다.
     * getFeedback()과 마찬가지로 호출자가 당사자인지 확인하는 로직은 없다.
     */
    @Operation(summary = "F-30 피드백 메시지 목록 조회", description = "메시지를 작성 시각 오름차순으로 조회합니다.")
    @GetMapping("/api/feedbacks/{id}/messages")
    public ResponseEntity<ApiResponse<List<FeedbackMessageResponse>>> getMessages(@PathVariable Long id) {
        List<FeedbackMessageResponse> response = feedbackService.getMessages(id);
        return ResponseEntity.ok(ApiResponse.success("메시지 목록 조회 성공", response));
    }

    /**
     * F-30: 로그인한 구독자 본인이 개설한 문의 스레드 목록을 조회한다.
     * 화면: S-11 구독자 뷰에서 "본인 문의 내역" 영역에 사용.
     */
    @Operation(summary = "F-30 내 문의 스레드 목록 조회 (구독자용)", description = "구독자 본인이 개설한 문의 스레드 목록.")
    @GetMapping("/api/feedbacks/me")
    public ResponseEntity<ApiResponse<MyFeedbackListResponse>> getMyFeedbacks(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        MyFeedbackListResponse response = feedbackService.getMyFeedbacks(userId);
        return ResponseEntity.ok(ApiResponse.success("내 문의 목록 조회 성공", response));
    }

    /**
     * 로그인한 전문가가 자신이 담당하는 문의 스레드 목록을 조회한다(S-11 EXPERT 화면 용도).
     * API 명세 F-30의 "관련 API" 목록에는 이 엔드포인트가 없어, 화면설계 S-11에 명시된
     * "EXPERT: 담당 문의 스레드 목록" 요구사항을 충족시키기 위해 개발 과정에서 추가된 것으로
     * 보인다. 명세에 정식으로 반영할지 팀 확인이 필요하다.
     */
    @Operation(summary = "받은 문의 목록 조회 (전문가용, 명세서 미확정)", description = "전문가로서 받은 문의 스레드 목록.")
    @GetMapping("/api/feedbacks/expert")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyExpertFeedbacks(Authentication authentication) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        List<FeedbackResponse> response = feedbackService.getMyExpertFeedbacks(userId);
        return ResponseEntity.ok(ApiResponse.success("받은 문의 목록 조회 성공", response));
    }
}