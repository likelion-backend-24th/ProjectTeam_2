package org.example.backend.study.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.exception.AuthErrorCode;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.common.file.FileStorageService;
import org.example.backend.common.file.ImageValidator;
import org.example.backend.study.dto.request.StudyPostRequest;
import org.example.backend.study.dto.response.StudyPostCommentResponse;
import org.example.backend.study.dto.response.StudyPostDetailResponse;
import org.example.backend.study.dto.response.StudyPostResponse;
import org.example.backend.study.dto.request.StudyPostUpdateRequest;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyPost;
import org.example.backend.study.entity.StudyPostImage;
import org.example.backend.study.exception.*;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostImageRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final StudyPostCommentService studyPostCommentService;
    private final StudyPostImageRepository studyPostImageRepository;
    private final FileStorageService fileStorageService;
    private final ImageValidator imageValidator;

    @Transactional
    public StudyPostResponse createStudyPost(Long userId, Long id, StudyPostRequest request, List<MultipartFile> images) {
        User user = getUserOrThrow(userId);
        Study study = getStudyOrThrow(id);
        validateStudyMember(id, userId);

        StudyPost post = new StudyPost(study, user, request.getTitle(), request.getContent());
        StudyPost saved = studyPostRepository.save(post);

        List<String> imageUrls = saveStudyPostImages(saved, images);

        return StudyPostResponse.from(saved, imageUrls);
    }

    public List<StudyPostResponse> getStudyPosts(Long userId, Long id) {
        getStudyOrThrow(id);
        validateStudyMember(id, userId);

        List<StudyPost> posts = studyPostRepository.findAllByStudyId(id);
        return posts.stream()
                .map(post -> StudyPostResponse.from(post, getImageUrls(post.getId())))
                .toList();
    }

    public StudyPostDetailResponse getStudyPostDetail(Long userId, Long id, Long postId) {
        getStudyOrThrow(id);
        validateStudyMember(id, userId);
        StudyPost post = getStudyPostOrThrow(id, postId);

        List<StudyPostCommentResponse> comments = studyPostCommentService.getStudyPostComments(userId, id, postId);
        List<String> imageUrls = getImageUrls(postId);

        return StudyPostDetailResponse.from(post, comments, imageUrls);
    }

    @Transactional
    public StudyPostResponse updateStudyPost(Long userId, Long id, Long postId, StudyPostUpdateRequest request) {
        getUserOrThrow(userId);
        getStudyOrThrow(id);
        validateStudyMember(id, userId);
        StudyPost post = getStudyPostOrThrow(id, postId);
        validatePostOwner(post, userId);

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        List<String> imageUrls = getImageUrls(postId);

        return StudyPostResponse.from(post, imageUrls);
    }

    @Transactional
    public void deleteStudyPost(Long userId, Long id, Long postId) {
        getStudyOrThrow(id);
        validateStudyMember(id, userId);
        StudyPost post = getStudyPostOrThrow(id, postId);
        validatePostOwner(post, userId);

        studyPostImageRepository.deleteAllByStudyPostId(postId);
        studyPostRepository.delete(post);
    }

    private List<String> saveStudyPostImages(StudyPost post, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            imageValidator.validate(file);
            String url = fileStorageService.upload(file, "study-posts");
            studyPostImageRepository.save(new StudyPostImage(post, url, file.getOriginalFilename(), i));
            imageUrls.add(url);
        }
        return imageUrls;
    }

    private List<String> getImageUrls(Long postId) {
        return studyPostImageRepository.findAllByStudyPostIdOrderByImageOrder(postId).stream()
                .map(StudyPostImage::getImageUrl)
                .toList();
    }

    // 검증 및 조회 메서드
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    private Study getStudyOrThrow(Long studyId) {
        return studyRepository.findById(studyId).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    private void validateStudyMember(Long studyId, Long userId) {
        studyMemberRepository.findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

    private StudyPost getStudyPostOrThrow(Long studyId, Long postId) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_POST_NOT_FOUND));

        if (!post.getStudy().getId().equals(studyId)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_MISMATCH);
        }
        return post;
    }

    private void validatePostOwner(StudyPost post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_POST_FORBIDDEN);
        }
    }
}