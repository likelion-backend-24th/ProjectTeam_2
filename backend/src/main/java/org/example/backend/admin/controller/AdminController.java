package org.example.backend.admin.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.admin.dto.AdminUserResponse;
import org.example.backend.admin.dto.UserStatusUpdateRequest;
import org.example.backend.admin.service.AdminService;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.common.dto.Meta;
import org.example.backend.common.dto.PageMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    //유저 목록 조회
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getUsers(
            @PageableDefault(size = 10)Pageable pageable){
        Page<AdminUserResponse> page = adminService.getUsers(pageable);
        Meta meta = Meta.builder().pagination(PageMeta.from(page)).build();
        return ResponseEntity.ok(ApiResponse.success("유저 목록 조회 성공",page.getContent(),meta));
    }

    //유저 상태 변경
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> changeUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request){
        adminService.changeUserStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("유저 상태가 변경되었습니다.", null));
    }

    //게시글 강제 삭제
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id){
        adminService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("게시글이 강제 삭제되었습니다.", null));
    }

    //댓글 강제 삭제
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id){
        adminService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.success("댓글이 강제 삭제되었습니다.", null));
    }

    //스터디 강제 삭제
    @DeleteMapping("/studies/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudy(@PathVariable Long id){
        adminService.deleteStudy(id);
        return ResponseEntity.ok(ApiResponse.success("스터디가 강제 삭제되었습니다.", null));
    }

}
