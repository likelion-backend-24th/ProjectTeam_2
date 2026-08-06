package org.example.backend.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FeedbackCreateRequest {

    @NotBlank(message = "topic은 필수입니다.")
    @Size(max = 100, message = "topic은 100자를 넘을 수 없습니다.")
    private String topic;

    @NotNull(message = "expertProfileId는 필수입니다.")
    private Long expertProfileId;

    @NotBlank(message = "content는 필수입니다.")
    @Size(max = 2000, message = "content는 2000자를 넘을 수 없습니다.")
    private String content;
}