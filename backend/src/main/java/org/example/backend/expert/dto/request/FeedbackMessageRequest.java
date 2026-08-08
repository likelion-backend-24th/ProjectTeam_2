package org.example.backend.expert.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FeedbackMessageRequest {

    @Schema(description = "메시지 내용", example = "네, 확인 후 다시 연락드릴게요.")
    @NotBlank(message = "content는 필수입니다.")
    @Size(max = 2000, message = "content는 2000자를 넘을 수 없습니다.")
    private String content;
}