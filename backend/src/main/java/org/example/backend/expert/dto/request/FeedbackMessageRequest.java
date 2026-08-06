package org.example.backend.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 문의 스레드에 메시지 추가(POST /api/feedbacks/{id}/messages) 요청 바디.
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedbackMessageRequest {

    @NotBlank(message = "content는 필수입니다.")
    @Size(max = 2000, message = "content는 2000자를 넘을 수 없습니다.")
    private String content;
}