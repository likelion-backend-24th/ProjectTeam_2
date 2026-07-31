package org.example.backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.comment.dto.CommentResponse; // comment 패키지의 dto 폴더에서 가져오기

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 상세 조회 응답 DTO
 * - 게시글 본문 정보 + 해당 게시글에 달린 댓글 목록을 함께 담는다 (F-05)
 * - 목록 조회(PostResponse)와 달리 댓글까지 포함하는 것이 차이점
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String authorNickname; // User 엔티티 전체 대신 닉네임만 노출 (보안)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> comments; // 게시글에 달린 댓글 목록
}