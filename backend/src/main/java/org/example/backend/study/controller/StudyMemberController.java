package org.example.backend.study.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "스터디 멤버", description = "스터디 가입, 멤버 조회, 강퇴, 방장 위임, 탈퇴 API")
public class StudyMemberController {

    private final StudyMemberService studyMemberService;

    @Operation(summary = "스터디 가입 신청", description = "해당 스터디에 멤버로 가입합니다. 방장 본인은 가입할 수 없고, 정원이 초과되면 가입할 수 없습니다.")
    @PostMapping("/members")
    public ResponseEntity<ApiResponse<StudyMemberResponse>> joinStudy(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        Long userId = user.getUser().getId();
        StudyMemberResponse response = studyMemberService.joinStudy(userId, id);
        return ResponseEntity.ok(ApiResponse.success("스터디 가입", response));
    }

    @Operation(summary = "스터디 멤버 목록 조회", description = "해당 스터디에 가입한 멤버 목록을 조회합니다.")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<StudyMemberResponse>>> getStudyMembers(@PathVariable Long id) {
        List<StudyMemberResponse> response = studyMemberService.getStudyMembers(id);
        return ResponseEntity.ok(ApiResponse.success("스터디 멤버 목록 조회 성공", response));
    }

    @Operation(summary = "스터디 멤버 강퇴", description = "방장 본인이 특정 멤버를 강퇴합니다. 방장 본인은 강퇴할 수 없습니다.")
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeStudyMember(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @PathVariable Long memberId) {
        Long userId = user.getUser().getId();
        studyMemberService.removeStudyMember(userId, id, memberId);
        return ResponseEntity.ok(ApiResponse.success("스터디 멤버 강퇴 완료", null));
    }

    @Operation(summary = "방장 위임", description = "방장 본인이 다른 멤버에게 방장 권한을 위임합니다. 본인에게는 위임할 수 없습니다.")
    @PatchMapping("/leader")
    public ResponseEntity<ApiResponse<Void>> delegateLeader(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, @Valid @RequestBody StudyLeaderDelegateRequest request) {
        Long userId = user.getUser().getId();
        Long newLeaderId = request.getNewLeaderId();
        studyMemberService.delegateLeader(userId, id, newLeaderId);
        return ResponseEntity.ok(ApiResponse.success("방장 위임 완료", null));
    }

    @Operation(summary = "스터디 탈퇴", description = "로그인한 사용자가 해당 스터디를 탈퇴합니다. 방장은 방장 위임을 먼저 해야 탈퇴할 수 있습니다.")
    @DeleteMapping("/leave")
    public ResponseEntity<ApiResponse<Void>> leaveStudy(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
        Long userId = user.getUser().getId();
        studyMemberService.leaveStudy(userId, id);
        return ResponseEntity.ok(ApiResponse.success("스터디 탈퇴 성공", null));
    }

}
