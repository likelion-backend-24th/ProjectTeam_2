package org.example.backend.study.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.dto.StudyPostRequest;
import org.example.backend.study.dto.StudyPostResponse;
import org.example.backend.study.dto.StudyPostUpdateRequest;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyPost;
import org.example.backend.study.exception.*;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;

    public StudyPostResponse createStudyPost(Long userId, Long id, StudyPostRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));

        StudyPost post = new StudyPost(study, user, request.getTitle(), request.getContent());
        StudyPost saved = studyPostRepository.save(post);

        return StudyPostResponse.from(saved);
    }

    public List<StudyPostResponse> getStudyPosts(Long userId, Long id) {
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
        studyMemberRepository.findByStudyIdAndUserId(id, userId).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));

        List<StudyPost> posts = studyPostRepository.findAllByStudyId(id);
        return posts.stream()
                .map(StudyPostResponse::from)
                .toList();
    }

    public StudyPostResponse getStudyPost(Long userId, Long id, Long postId) {
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
        studyMemberRepository.findByStudyIdAndUserId(id, userId).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
        StudyPost post = studyPostRepository.findById(postId).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_POST_NOT_FOUND));

        if (!post.getStudy().getId().equals(id)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_MISMATCH);
        }

        return StudyPostResponse.from(post);
    }

    @Transactional
    public StudyPostResponse updateStudyPost(Long userId, Long id, Long postId, StudyPostUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
        studyMemberRepository.findByStudyIdAndUserId(id, userId).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
        StudyPost post = studyPostRepository.findById(postId).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_POST_NOT_FOUND));
        if (!post.getStudy().getId().equals(id)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_MISMATCH);
        }

        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_FORBIDDEN);
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        return StudyPostResponse.from(post);
    }

    @Transactional
    public void deleteStudyPost(Long userId, Long id, Long postId) {
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
        studyMemberRepository.findByStudyIdAndUserId(id, userId).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
        StudyPost post = studyPostRepository.findById(postId).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_POST_NOT_FOUND));

        if (!post.getStudy().getId().equals(id)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_MISMATCH);
        }

        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_FORBIDDEN);
        }

        studyPostRepository.delete(post);
    }
}
