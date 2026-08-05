package org.example.backend.study.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.exception.AuthErrorCode;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.dto.StudyPostCommentRequest;
import org.example.backend.study.dto.StudyPostCommentResponse;
import org.example.backend.study.dto.StudyPostCommentUpdateRequest;
import org.example.backend.study.entity.StudyPost;
import org.example.backend.study.entity.StudyPostComment;
import org.example.backend.study.exception.StudyErrorCode;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPostCommentService {

    private final StudyPostCommentRepository studyPostCommentRepository;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudyPostRepository studyPostRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional
    public StudyPostCommentResponse createStudyPostComment(Long userId, Long id, Long postId, StudyPostCommentRequest request) {
        User user = getUserOrThrow(userId);
        getStudyOrThrow(id);
        validateStudyMember(id, userId);
        StudyPost post = getStudyPostOrThrow(id, postId);

        StudyPostComment comment = new StudyPostComment(post, user, request.getContent());
        StudyPostComment saved = studyPostCommentRepository.save(comment);
        return StudyPostCommentResponse.from(saved);
    }

    public List<StudyPostCommentResponse> getStudyPostComments(Long userId, Long id, Long postId) {
        getUserOrThrow(userId);
        getStudyOrThrow(id);
        validateStudyMember(id, userId);
        getStudyPostOrThrow(id, postId);

        List<StudyPostComment> comments = studyPostCommentRepository.findAllByStudyPostId(postId);
        return comments.stream()
                .map(StudyPostCommentResponse::from)
                .toList();
    }

    @Transactional
    public StudyPostCommentResponse updateStudyPostComment(Long userId, Long id, Long postId, Long commentId, StudyPostCommentUpdateRequest request) {
        getUserOrThrow(userId);
        getStudyOrThrow(id);
        validateStudyMember(id, userId);
        getStudyPostOrThrow(id, postId);

        StudyPostComment comment = getStudyPostCommentOrThrow(postId, commentId);
        validateStudyPostCommentOwner(comment, userId);

        comment.setContent(request.getContent());
        return StudyPostCommentResponse.from(comment);
    }

    @Transactional
    public void deleteStudyPostComment(Long userId, Long id, Long postId, Long commentId) {
        getUserOrThrow(userId);
        getStudyOrThrow(id);
        validateStudyMember(id, userId);
        getStudyPostOrThrow(id, postId);
        StudyPostComment comment = getStudyPostCommentOrThrow(postId, commentId);
        validateStudyPostCommentOwner(comment, userId);

        studyPostCommentRepository.delete(comment);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    private void getStudyOrThrow(Long id) {
        studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    private StudyPost getStudyPostOrThrow(Long studyId, Long postId) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_POST_NOT_FOUND));

        if (!post.getStudy().getId().equals(studyId)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_MISMATCH);
        }
        return post;
    }

    private void validateStudyMember(Long studyId, Long userId) {
        studyMemberRepository.findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    private StudyPostComment getStudyPostCommentOrThrow(Long postId, Long commentId) {
        StudyPostComment comment = studyPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_POST_COMMENT_NOT_FOUND));

        if (!comment.getStudyPost().getId().equals(postId)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_MISMATCH);
        }
        return comment;
    }

    private void validateStudyPostCommentOwner(StudyPostComment comment, Long userId) {
        if(!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_COMMENT_FORBIDDEN);
        }
    }

}