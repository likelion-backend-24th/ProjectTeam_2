package org.example.backend.report.service;

import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.report.dto.ReportCreateRequest;
import org.example.backend.report.entity.Report;
import org.example.backend.report.entity.ReportReason;
import org.example.backend.report.entity.ReportStatus;
import org.example.backend.report.entity.ReportTargetType;
import org.example.backend.report.exception.ReportErrorCode;
import org.example.backend.report.repository.ReportRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.example.backend.expert.repository.FeedbackRepository;
import org.example.backend.expert.service.FeedbackService;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private StudyPostRepository studyPostRepository;

    @Mock
    private StudyPostCommentRepository studyPostCommentRepository;

    @InjectMocks
    private ReportService reportService;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private FeedbackService feedbackService;

    @Test
    void 신고_대상이_존재하지_않으면_예외가_발생한다() {
        when(commentRepository.existsById(12L)).thenReturn(false);

        User reporter = new User();
        reporter.setId(1L);

        ReportCreateRequest request = new ReportCreateRequest(
                ReportTargetType.COMMENT, 12L, ReportReason.SPAM, "테스트입니다.");

        assertThatThrownBy(() -> reportService.createReport(request, reporter))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ReportErrorCode.REPORT_TARGET_NOT_FOUND);
    }

    @Test
    void 이미_신고한_대상이면_예외가_발생한다() {
        when(commentRepository.existsById(12L)).thenReturn(true);
        when(reportRepository.existsByTargetTypeAndTargetIdAndReporterIdAndStatus(
                ReportTargetType.COMMENT, 12L, 1L, ReportStatus.PENDING)).thenReturn(true);

        User reporter = new User();
        reporter.setId(1L);

        ReportCreateRequest request = new ReportCreateRequest(
                ReportTargetType.COMMENT, 12L, ReportReason.SPAM, "테스트입니다.");

        assertThatThrownBy(() -> reportService.createReport(request, reporter))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ReportErrorCode.REPORT_ALREADY_EXISTS);
    }

    @Test
    void 정상적인_신고는_저장까지_이어진다() {
        when(commentRepository.existsById(12L)).thenReturn(true);
        when(reportRepository.existsByTargetTypeAndTargetIdAndReporterIdAndStatus(
                ReportTargetType.COMMENT, 12L, 1L, ReportStatus.PENDING)).thenReturn(false);
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User reporter = new User();
        reporter.setId(1L);
        reporter.setNickname("테스터");

        ReportCreateRequest request = new ReportCreateRequest(
                ReportTargetType.COMMENT, 12L, ReportReason.SPAM, "테스트입니다.");

        reportService.createReport(request, reporter);

        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void 존재하지_않는_신고를_처리하려_하면_예외가_발생한다() {
        when(reportRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.resolveReport(99999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ReportErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    void 피드백_스레드_신고시_당사자가_아니면_예외가_발생한다() {
        when(feedbackRepository.existsById(5L)).thenReturn(true);
        when(feedbackService.isParticipant(5L, 1L)).thenReturn(false);

        User reporter = new User();
        reporter.setId(1L);

        ReportCreateRequest request = new ReportCreateRequest(
                ReportTargetType.FEEDBACK, 5L, ReportReason.ABUSE, "당사자 아닌데 신고 시도");

        assertThatThrownBy(() -> reportService.createReport(request, reporter))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ReportErrorCode.REPORT_ACCESS_DENIED);

        verify(reportRepository, never()).save(any());
    }

    @Test
    void 피드백_스레드_신고시_당사자면_정상처리된다() {
        when(feedbackRepository.existsById(5L)).thenReturn(true);
        when(feedbackService.isParticipant(5L, 1L)).thenReturn(true);
        when(reportRepository.existsByTargetTypeAndTargetIdAndReporterIdAndStatus(
                ReportTargetType.FEEDBACK, 5L, 1L, ReportStatus.PENDING)).thenReturn(false);
        when(reportRepository.save(any(Report.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User reporter = new User();
        reporter.setId(1L);
        reporter.setNickname("테스터");

        ReportCreateRequest request = new ReportCreateRequest(
                ReportTargetType.FEEDBACK, 5L, ReportReason.ABUSE, "부적절한 상담이었습니다.");

        reportService.createReport(request, reporter);

        verify(reportRepository).save(any(Report.class));
    }
}