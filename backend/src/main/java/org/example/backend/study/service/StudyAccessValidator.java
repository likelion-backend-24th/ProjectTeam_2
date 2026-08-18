package org.example.backend.study.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.exception.AuthErrorCode;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyPost;
import org.example.backend.study.exception.StudyErrorCode;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudyAccessValidator {

    private static final int FREE_TIER_STUDY_LIMIT = 2;

    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudyPostRepository studyPostRepository;
    private final StudyMemberRepository studyMemberRepository;

    public User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    public Study getStudyOrThrow(Long studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    public StudyPost getStudyPostOrThrow(Long studyId, Long postId) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_POST_NOT_FOUND));

        if (!post.getStudy().getId().equals(studyId)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_MISMATCH);
        }
        return post;
    }

    public void validateStudyMember(Long studyId, Long userId) {
        studyMemberRepository.findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    public void validateStudyLeader(Study study, Long userId) {
        if (!study.getLeader().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    public void validateFreeTierLimit(User user) {
        if (!user.isSubscribed()) {
            int joinedStudyCount = studyMemberRepository.countByUserId(user.getId());
            if (joinedStudyCount >= FREE_TIER_STUDY_LIMIT) {
                throw new BusinessException(StudyErrorCode.STUDY_JOIN_LIMIT_EXCEEDED);
            }
        }
    }
}
