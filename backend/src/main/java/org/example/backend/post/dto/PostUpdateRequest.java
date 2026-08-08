package org.example.backend.post.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.post.entity.PostCategory;

@Getter
@NoArgsConstructor
@AllArgsConstructor

public class PostUpdateRequest {

    @Schema(description = "수정할 게시글 제목", example = "신입 개발자 면접 후기 공유합니다 (수정)")
    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @Schema(description = "수정할 게시글 내용", example = "내용을 좀 더 자세히 보완했습니다...(수정)")
    @NotBlank(message = "내용은 필수 입력 항목입니다.")
    private String content;

    @Schema(description = "수정할 게시글 카테고리", example = "INTERVIEW_REVIEW")
    @NotNull(message = "카테고리를 선택하지 않았습니다")
    private PostCategory category;
}
