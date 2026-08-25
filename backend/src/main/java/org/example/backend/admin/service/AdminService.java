package org.example.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.admin.dto.AdminUserResponse;
import org.example.backend.admin.exception.AdminErrorCode;
import org.example.backend.auth.repository.RefreshTokenRepository;
import org.example.backend.comment.service.CommentService;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.expert.dto.response.ExpertProfileResponse;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.expert.service.ExpertProfileService;
import org.example.backend.post.service.PostService;
import org.example.backend.report.dto.ReportResponse;
import org.example.backend.report.entity.ReportStatus;
import org.example.backend.report.service.ReportService;
import org.example.backend.study.service.StudyPostCommentService;
import org.example.backend.study.service.StudyPostService;
import org.example.backend.study.service.StudyService;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.backend.expert.dto.response.FeedbackMessageResponse;
import org.example.backend.expert.dto.response.FeedbackResponse;
import org.example.backend.expert.service.FeedbackService;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PostService postService;
    private final CommentService commentService;
    private final ReportService reportService;
    private final ExpertProfileService expertProfileService;
    private final FeedbackService feedbackService;
    private final StudyService studyService;
    private final StudyPostService studyPostService;
    private final StudyPostCommentService studyPostCommentService;
    private final RefreshTokenRepository refreshTokenRepository;
    //유저 목록 조회
    public Page<AdminUserResponse> getUsers(String keyword, Role role, Pageable pageable){
        return userRepository.searchUsers(keyword, role, pageable)
                .map(user -> AdminUserResponse.from(user));
    }

    //유저 상태 변경
    @Transactional
    public void changeUserStatus(Long userId, AccountStatus status){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.USER_NOT_FOUND));

        if(status == AccountStatus.WITHDRAWN){
            throw new BusinessException(AdminErrorCode.INVALID_STATUS_CHANGE);
        }

        user.setStatus(status);
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);
    }

    // 게시글 소프트 딜리트 (게시물에 딸린 댓글도 소프트 딜리트)
    @Transactional
    public void deletePost(Long postId){
        postService.adminDeletePost(postId);
    }

    //댓글 소프트 딜리트
    @Transactional
    public void deleteComment(Long commentId){
        commentService.adminDeleteComment(commentId);
    }

    //스터디 소프트 딜리트 (연관된 게시글, 댓글, 멤버까지)
    @Transactional
    public void deleteStudy(Long studyId){
        studyService.adminDeleteStudy(studyId);
    }

    //스터디 게시글 소프트 딜리트 (연관된 댓글까지 함께)
    @Transactional
    public void deleteStudyPost(Long studyPostId){
        studyPostService.adminDeleteStudyPost(studyPostId);
    }

    //스터디 게시글 댓글 소프트 딜리트
    @Transactional
    public void deleteStudyPostComment(Long commentId){
        studyPostCommentService.adminDeleteStudyPostComment(commentId);
    }

    // 전문가 목록 조회
    public Page<ExpertProfileResponse> getList(ExpertStatus status, Pageable pageable) {
        return expertProfileService.getList(status, pageable);
    }

    // 전문가 승인
    public ExpertProfileResponse approve(Long expertProfileId) {
        return expertProfileService.approve(expertProfileId);
    }

    // 전문가 거절
    public ExpertProfileResponse reject(Long expertProfileId, String reason) {
        return expertProfileService.reject(expertProfileId, reason);
    }

    // 전문가 자격 박탈
    public void revoke(Long expertProfileId, String reason) {
        expertProfileService.revoke(expertProfileId, reason);
    }

    //신고 목록 조회 (status로 필터링, status가 null이면 전체 조회)
    public Page<ReportResponse> getReports(ReportStatus status, Pageable pageable){
        return reportService.getReports(status, pageable);
    }

    //신고 처리 - 신고 대상 콘텐츠를 삭제하면서 종료 (PENDING → DELETED)
    @Transactional
    public void resolveReport(Long reportId){
        reportService.resolveReport(reportId);
    }

    //신고 처리 - 콘텐츠는 유지하고 반려 (PENDING → REJECTED)
    @Transactional
    public void rejectReport(Long reportId){
        reportService.rejectReport(reportId);
    }

    // 신고된 피드백 스레드 상세 조회 (신고가 접수된 스레드만 열람 가능)
    public FeedbackResponse getFeedback(Long feedbackId) {
        return feedbackService.getFeedbackForAdmin(feedbackId);
    }

    // 신고된 피드백 스레드의 메시지 목록 조회
    public List<FeedbackMessageResponse> getFeedbackMessages(Long feedbackId) {
        return feedbackService.getMessagesForAdmin(feedbackId);
    }

}
