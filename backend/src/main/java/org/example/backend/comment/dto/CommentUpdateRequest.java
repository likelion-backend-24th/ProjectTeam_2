package org.example.backend.comment.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateRequest {

    @Schema(description = "수정할 댓글 내용", example = "유용하네요 ㅋㅋ")
    @NotBlank(message = "수정할 댓글 내용을 입력해주세요.")
    private String content;
}
