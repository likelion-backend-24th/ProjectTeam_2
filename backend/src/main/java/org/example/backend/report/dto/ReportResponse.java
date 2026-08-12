package org.example.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.report.entity.Report;
import org.example.backend.report.entity.ReportReason;
import org.example.backend.report.entity.ReportStatus;
import org.example.backend.report.entity.ReportTargetType;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponse {

    @Schema(description = "신고 ID", example = "1")
    private Long id;

    @Schema(description = "신고 대상 종류", example = "COMMENT")
    private ReportTargetType targetType;

    @Schema(description = "신고 대상 ID", example = "12")
    private Long targetId;

    @Schema(description = "신고자 닉네임", example = "안양개발자")
    private String reporterNickname;

    @Schema(description = "신고 사유", example = "SPAM")
    private ReportReason reason;

    @Schema(description = "상세 사유", example = "지각비를 걷으려 지각을 부추기며 종용하기에 신고했습니다.")
    private String detail;

    @Schema(description = "처리 상태", example = "PENDING")
    private ReportStatus status;

    @Schema(description = "신고 접수일시", example = "2026-08-11T10:00:00")
    private LocalDateTime createdAt;

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reporterNickname(report.getReporter().getNickname())
                .reason(report.getReason())
                .detail(report.getDetail())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}