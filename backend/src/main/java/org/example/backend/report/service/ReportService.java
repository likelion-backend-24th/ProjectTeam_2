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
        return reports.map(this::toResponseWithTargetSnapshot);
    }

    // 관리자가 신고 목록에서 뭘 신고당했는지 바로 볼 수 있도록 대상 콘텐츠의 제목/내용/작성자를 채워 넣는다.
    // 이미 삭제 처리된 대상은 SoftDelete 필터에 걸려 조회가 안 되므로 안내 문구로 대체한다.
    private ReportResponse toResponseWithTargetSnapshot(Report report) {
        ReportResponse.ReportResponseBuilder builder = ReportResponse.from(report).toBuilder();

        switch (report.getTargetType()) {
            case POST -> postRepository.findById(report.getTargetId()).ifPresentOrElse(
                    post -> builder.targetTitle(post.getTitle())
                            .targetContentPreview(truncate(post.getContent()))
                            .targetAuthorNickname(post.getUser().getNickname()),
                    () -> builder.targetContentPreview("삭제된 게시글입니다."));
            case COMMENT -> commentRepository.findById(report.getTargetId()).ifPresentOrElse(
                    comment -> builder.targetContentPreview(truncate(comment.getContent()))
                            .targetAuthorNickname(comment.getUser().getNickname()),
                    () -> builder.targetContentPreview("삭제된 댓글입니다."));
            case STUDY_POST -> studyPostRepository.findById(report.getTargetId()).ifPresentOrElse(
                    studyPost -> builder.targetTitle(studyPost.getTitle())
                            .targetContentPreview(truncate(studyPost.getContent()))
                            .targetAuthorNickname(studyPost.getUser().getNickname()),
                    () -> builder.targetContentPreview("삭제된 스터디 게시글입니다."));
            case STUDY_POST_COMMENT -> studyPostCommentRepository.findById(report.getTargetId()).ifPresentOrElse(
                    comment -> builder.targetContentPreview(truncate(comment.getContent()))
                            .targetAuthorNickname(comment.getUser().getNickname()),
                    () -> builder.targetContentPreview("삭제된 댓글입니다."));
            case FEEDBACK -> feedbackRepository.findById(report.getTargetId()).ifPresentOrElse(
                    feedback -> builder.targetTitle(feedback.getTopic())
                            .targetAuthorNickname(feedback.getRequester().getNickname()),
                    () -> builder.targetContentPreview("존재하지 않는 상담입니다."));
        }

        return builder.build();
    }

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }

    // 신고 대상 콘텐츠를 삭제 처리하면서 신고를 종료할 때
    @Transactional
    public void resolveReport(Long reportId) {
        Report report = getReportOrThrow(reportId);
        report.setStatus(ReportStatus.DELETED);
        report.setResolvedAt(java.time.LocalDateTime.now());
    }

    // 콘텐츠는 그대로 두고 신고를 반려할 때
    @Transactional
    public void rejectReport(Long reportId) {
        Report report = getReportOrThrow(reportId);
        report.setStatus(ReportStatus.REJECTED);
        report.setResolvedAt(java.time.LocalDateTime.now());
    }

    private Report getReportOrThrow(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));
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