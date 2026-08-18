package org.example.backend.report.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.report.dto.ReportCreateRequest;
import org.example.backend.report.dto.ReportResponse;
import org.example.backend.report.entity.Report;
import org.example.backend.report.entity.ReportStatus;
import org.example.backend.report.exception.ReportErrorCode;
import org.example.backend.report.repository.ReportRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.backend.expert.repository.FeedbackRepository;
import org.example.backend.expert.service.FeedbackService;
import org.example.backend.report.entity.ReportTargetType;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository studyPostCommentRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackService feedbackService;

    @Transactional
    public ReportResponse createReport(ReportCreateRequest request, User reporter) {
        validateTargetExists(request.getTargetType(), request.getTargetId());
        validateReportPermission(request.getTargetType(), request.getTargetId(), reporter.getId());

        if (reportRepository.existsByTargetTypeAndTargetIdAndReporterId(
                request.getTargetType(), request.getTargetId(), reporter.getId())) {
            throw new BusinessException(ReportErrorCode.REPORT_ALREADY_EXISTS);
        }

        Report report = new Report();
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setDetail(request.getDetail());
        report.setStatus(ReportStatus.PENDING);

        return ReportResponse.from(reportRepository.save(report));
    }

    public Page<ReportResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<Report> reports = (status == null)
                ? reportRepository.findAll(pageable)
                : reportRepository.findAllByStatus(status, pageable);
        return reports.map(ReportResponse::from);
    }

    @Transactional
    public void resolveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));
        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(java.time.LocalDateTime.now());
    }

    private void validateTargetExists(org.example.backend.report.entity.ReportTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            case STUDY_POST -> studyPostRepository.existsById(targetId);
            case STUDY_POST_COMMENT -> studyPostCommentRepository.existsById(targetId);
            case FEEDBACK -> feedbackRepository.existsById(targetId);
        };

        if (!exists) {
            throw new BusinessException(ReportErrorCode.REPORT_TARGET_NOT_FOUND);
        }
    }

    private void validateReportPermission(ReportTargetType targetType, Long targetId, Long reporterId) {
        if (targetType == ReportTargetType.FEEDBACK && !feedbackService.isParticipant(targetId, reporterId)) {
            throw new BusinessException(ReportErrorCode.REPORT_ACCESS_DENIED);
        }
    }

}