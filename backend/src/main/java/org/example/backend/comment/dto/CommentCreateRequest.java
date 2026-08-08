package org.example.backend.comment.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor

public class CommentCreateRequest {

    @Schema(description = "댓글 내용", example = "좋은 정보입니다~")
    @NotBlank(message = "댓글 내용이 없습니다.")
    private String content;
}
