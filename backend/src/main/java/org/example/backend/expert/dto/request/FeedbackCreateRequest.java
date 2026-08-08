package org.example.backend.expert.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "문의 주제", example = "이력서 첨삭 부탁드립니다")
    @NotBlank(message = "topic은 필수입니다.")
    @Size(max = 100, message = "topic은 100자를 넘을 수 없습니다.")
    private String topic;

    @Schema(description = "문의할 전문가 프로필 ID", example = "1")
    @NotNull(message = "expertProfileId는 필수입니다.")
    private Long expertProfileId;

    @Schema(description = "문의 내용 (최초 메시지)", example = "이력서 초안 첨부드립니다. 검토 부탁드려요.")
    @NotBlank(message = "content는 필수입니다.")
    @Size(max = 2000, message = "content는 2000자를 넘을 수 없습니다.")
    private String content;
}