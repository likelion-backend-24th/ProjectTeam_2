package org.example.backend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class StudyPostRequest {

    @Schema(description = "게시글 제목", example = "이번 주 스터디 공지")
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @Schema(description = "게시글 내용", example = "이번 주는 화요일에 모입니다. 지각 시 분당 1000원 벌금입니다.")
    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 5000, message = "내용은 5000자를 초과할 수 없습니다.")
    private String content;

}