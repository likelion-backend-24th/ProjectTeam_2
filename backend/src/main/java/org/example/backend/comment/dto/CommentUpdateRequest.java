package org.example.backend.comment.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateRequest {
    @NotBlank(message = "수정할 댓글 내용을 입력해주세요.")
    private String content;
}
