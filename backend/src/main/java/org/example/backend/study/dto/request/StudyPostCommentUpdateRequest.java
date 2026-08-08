package org.example.backend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class StudyPostCommentUpdateRequest {

    @Schema(description = "수정할 댓글 내용", example = "수요일로 알겠습니다!")
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 1000, message = "댓글은 1000자를 초과할 수 없습니다.")
    private String content;

}