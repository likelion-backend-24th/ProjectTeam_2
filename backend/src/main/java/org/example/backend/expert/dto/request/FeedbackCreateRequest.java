package org.example.backend.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 문의 스레드 개설(POST /api/feedbacks) 요청 바디.
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedbackCreateRequest {

    @NotNull(message = "expertProfileId는 필수입니다.")
    private Long expertProfileId;

    @NotBlank(message = "content는 필수입니다.")
    @Size(max = 2000, message = "content는 2000자를 넘을 수 없습니다.")
    private String content;
}