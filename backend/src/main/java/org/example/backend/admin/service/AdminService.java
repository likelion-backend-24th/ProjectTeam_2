package org.example.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.admin.dto.AdminUserResponse;
import org.example.backend.admin.exception.AdminErrorCode;
import org.example.backend.comment.entity.Comment;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.post.entity.Post;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.report.dto.ReportResponse;
import org.example.backend.report.entity.ReportStatus;
import org.example.backend.report.service.ReportService;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyPost;
import org.example.backend.study.entity.StudyPostComment;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StudyRepository studyRepository;
    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository studyPostCommentRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final ReportService reportService;

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
    }

    //유저 role 변경
    @Transactional
    public void changeUserRole(Long userId, Role role){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.USER_NOT_FOUND));
        user.setRole(role);
        userRepository.save(user);
    }

    // 게시글 소프트 딜리트 (게시물에 딸린 댓글도 소프트 딜리트)
    @Transactional
    public void deletePost(Long postId){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.POST_NOT_FOUND));

        List<Comment> comments = commentRepository.findAllByPostId(postId);
        commentRepository.deleteAll(comments);

        postRepository.delete(post);
    }

    //댓글 소프트 딜리트
    @Transactional
    public void deleteComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.COMMENT_NOT_FOUND));

        commentRepository.delete(comment);
    }

    //스터디 소프트 딜리트 (연관된 게시글, 댓글, 멤버까지)
    @Transactional
    public void deleteStudy(Long studyId){
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.STUDY_NOT_FOUND));

        List<StudyPost> studyPosts = studyPostRepository.findAllByStudyId(studyId);
        for (StudyPost studyPost : studyPosts) {
            List<StudyPostComment> comments = studyPostCommentRepository.findAllByStudyPostId(studyPost.getId());
            studyPostCommentRepository.deleteAll(comments);
        }
        studyPostRepository.deleteAll(studyPosts);
        //멤버는 강제삭제
        studyMemberRepository.deleteByStudyId(studyId);

        studyRepository.delete(study);
    }

    //스터디 게시글 소프트 딜리트 (연관된 댓글까지 함께)
    @Transactional
    public void deleteStudyPost(Long studyPostId){
        StudyPost studyPost = studyPostRepository.findById(studyPostId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.STUDY_POST_NOT_FOUND));

        List<StudyPostComment> comments = studyPostCommentRepository.findAllByStudyPostId(studyPostId);
        studyPostCommentRepository.deleteAll(comments);

        studyPostRepository.delete(studyPost);
    }

    //스터디 게시글 댓글 소프트 딜리트
    @Transactional
    public void deleteStudyPostComment(Long commentId){
        StudyPostComment comment = studyPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.STUDY_POST_COMMENT_NOT_FOUND));

        studyPostCommentRepository.delete(comment);
    }

    //신고 목록 조회 (status로 필터링, status가 null이면 전체 조회)
    public Page<ReportResponse> getReports(ReportStatus status, Pageable pageable){
        return reportService.getReports(status, pageable);
    }

    //신고 처리 (PENDING → RESOLVED)
    @Transactional
    public void resolveReport(Long reportId){
        reportService.resolveReport(reportId);
    }

}
