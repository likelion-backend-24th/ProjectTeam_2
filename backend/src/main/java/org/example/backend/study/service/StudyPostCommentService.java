package org.example.backend.study.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.dto.request.StudyPostCommentRequest;
import org.example.backend.study.dto.request.StudyPostCommentUpdateRequest;
import org.example.backend.study.dto.response.StudyPostCommentResponse;
import org.example.backend.study.entity.StudyPost;
import org.example.backend.study.entity.StudyPostComment;
import org.example.backend.study.exception.StudyErrorCode;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPostCommentService {

    private final StudyPostCommentRepository studyPostCommentRepository;
    private final StudyAccessValidator studyAccessValidator;

    @Transactional
    public StudyPostCommentResponse createStudyPostComment(Long userId, Long id, Long postId, StudyPostCommentRequest request) {
        User user = studyAccessValidator.getUserOrThrow(userId);
        studyAccessValidator.getStudyOrThrow(id);
        studyAccessValidator.validateStudyMember(id, userId);
        StudyPost post = studyAccessValidator.getStudyPostOrThrow(id, postId);

        StudyPostComment comment = new StudyPostComment(post, user, request.getContent());
        StudyPostComment saved = studyPostCommentRepository.save(comment);
        return StudyPostCommentResponse.from(saved);
    }

    public List<StudyPostCommentResponse> getStudyPostComments(Long userId, Long id, Long postId) {
        List<StudyPostComment> comments = studyPostCommentRepository.findAllByStudyPostId(postId);
        return comments.stream()
                .map(StudyPostCommentResponse::from)
                .toList();
    }

    @Transactional
    public StudyPostCommentResponse updateStudyPostComment(Long userId, Long id, Long postId, Long commentId, StudyPostCommentUpdateRequest request) {
        studyAccessValidator.getUserOrThrow(userId);
        studyAccessValidator.getStudyOrThrow(id);
        studyAccessValidator.validateStudyMember(id, userId);
        studyAccessValidator.getStudyPostOrThrow(id, postId);

        StudyPostComment comment = getStudyPostCommentOrThrow(postId, commentId);
        validateStudyPostCommentOwner(comment, userId);

        comment.setContent(request.getContent());
        return StudyPostCommentResponse.from(comment);
    }

    @Transactional
    public void deleteStudyPostComment(Long userId, Long id, Long postId, Long commentId) {
        studyAccessValidator.getUserOrThrow(userId);
        studyAccessValidator.getStudyOrThrow(id);
        studyAccessValidator.validateStudyMember(id, userId);
        studyAccessValidator.getStudyPostOrThrow(id, postId);
        StudyPostComment comment = getStudyPostCommentOrThrow(postId, commentId);
        validateStudyPostCommentOwner(comment, userId);

        studyPostCommentRepository.delete(comment);
    }

    @Transactional
    public void adminDeleteStudyPostComment(Long commentId) {
        StudyPostComment comment = studyPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_POST_COMMENT_NOT_FOUND));
        studyPostCommentRepository.delete(comment);
    }

    @Transactional
    public void deleteAllByStudyPost(Long studyPostId) {
        List<StudyPostComment> comments = studyPostCommentRepository.findAllByStudyPostId(studyPostId);
        studyPostCommentRepository.deleteAll(comments);
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