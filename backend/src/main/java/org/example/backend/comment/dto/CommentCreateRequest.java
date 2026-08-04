package org.example.backend.comment.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor

public class CommentCreateRequest {
    @NotBlank(message = "댓글 내용이 없습니다.")
    private String content;
}
