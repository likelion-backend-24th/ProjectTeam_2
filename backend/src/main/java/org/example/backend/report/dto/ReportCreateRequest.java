package org.example.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.report.entity.ReportReason;
import org.example.backend.report.entity.ReportTargetType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequest {

    @Schema(description = "신고 대상 종류", example = "COMMENT")
    @NotNull(message = "신고 대상 종류를 선택해주세요.")
    private ReportTargetType targetType;

    @Schema(description = "신고 대상 ID", example = "12")
    @NotNull(message = "신고 대상을 선택해주세요.")
    private Long targetId;

    @Schema(description = "신고 사유", example = "SPAM")
    @NotNull(message = "신고 사유를 선택해주세요.")
    private ReportReason reason;

    @Schema(description = "상세 사유(선택)", example = "동일한 광고 댓글을 여러 게시글에 반복해서 올렸습니다.")
    private String detail;
}