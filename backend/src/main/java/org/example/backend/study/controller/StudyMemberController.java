package org.example.backend.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.study.dto.request.StudyLeaderDelegateRequest;
import org.example.backend.study.dto.response.StudyMemberResponse;
import org.example.backend.study.service.StudyMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies/{id}")
@RequiredArgsConstructor
public class StudyMemberController {

    private final StudyMemberService studyMemberService;

    @Operation(summary = "스터디 가입 신청")
    @PostMapping("/members")
    public ResponseEntity<ApiResponse<StudyMemberResponse>> joinStudy(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        Long userId = user.getUser().getId();
        StudyMemberResponse response = studyMemberService.joinStudy(userId, id);
        return ResponseEntity.ok(ApiResponse.success("스터디 가입", response));
    }

    @Operation(summary = "스터디 멤버 목록 조회")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<StudyMemberResponse>>> getStudyMembers(@PathVariable Long id) {
        List<StudyMemberResponse> response = studyMemberService.getStudyMembers(id);
        return ResponseEntity.ok(ApiResponse.success("스터디 멤버 목록 조회 성공", response));
    }

    @Operation(summary = "스터디 멤버 강퇴")
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeStudyMember(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @PathVariable Long memberId) {
        Long userId = user.getUser().getId();
        studyMemberService.removeStudyMember(userId, id, memberId);
        return ResponseEntity.ok(ApiResponse.success("스터디 멤버 강퇴 완료", null));
    }

    @Operation(summary = "방장 위임")
    @PatchMapping("/leader")
    public ResponseEntity<ApiResponse<Void>> delegateLeader(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @Valid @RequestBody StudyLeaderDelegateRequest request) {
        Long userId = user.getUser().getId();
        Long newLeaderId = request.getNewLeaderId();
        studyMemberService.delegateLeader(userId, id, newLeaderId);
        return ResponseEntity.ok(ApiResponse.success("방장 위임 완료", null));
    }

    @Operation(summary = "스터디 탈퇴")
    @DeleteMapping("/leave")
    public ResponseEntity<ApiResponse<Void>> leaveStudy(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        Long userId = user.getUser().getId();
        studyMemberService.leaveStudy(userId, id);
        return null;
    }

}
