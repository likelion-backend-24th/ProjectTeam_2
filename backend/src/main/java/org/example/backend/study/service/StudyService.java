package org.example.backend.study.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.common.file.FileStorageService;
import org.example.backend.common.file.ImageValidator;
import org.example.backend.study.dto.request.StudyRequest;
import org.example.backend.study.dto.request.StudyUpdateRequest;
import org.example.backend.study.dto.response.StudyDetailResponse;
import org.example.backend.study.dto.response.StudyResponse;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyImage;
import org.example.backend.study.entity.StudyMember;
import org.example.backend.study.exception.StudyErrorCode;
import org.example.backend.study.repository.StudyImageRepository;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyService {
    private final StudyRepository studyRepository;
    private final StudyPostService studyPostService;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyImageRepository studyImageRepository;
    private final FileStorageService fileStorageService;
    private final ImageValidator imageValidator;
    private final StudyAccessValidator studyAccessValidator;

    @Transactional
    public StudyResponse createStudy(Long userId, StudyRequest request, List<MultipartFile> images) {
        User user = studyAccessValidator.getUserOrThrow(userId);
        studyAccessValidator.validateFreeTierLimit(user);

        Study study = new Study(request.getTitle(), request.getDescription(), request.getCapacity(), LocalDate.now(), request.getRecruitEnd(), user, request.getCategory());

        Study saved = studyRepository.save(study);
        studyMemberRepository.save(new StudyMember(saved, user));

        List<String> imageUrls = saveStudyImages(saved, images);

        return StudyResponse.from(saved, 1, imageUrls);
    }

    public Page<StudyResponse> getStudies(String keyword, Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
        Page<Study> page = studyRepository.findAllOrderBySubscribedLeader(normalizedKeyword, pageable);

        Map<Long, Integer> memberCountMap = getMemberCountMap(page.getContent());

        return page.map(study -> {
            int memberCount = memberCountMap.getOrDefault(study.getId(), 0);
            List<String> imageUrls = getImageUrls(study.getId());
            return StudyResponse.from(study, memberCount, imageUrls);
        });
    }

    public StudyDetailResponse getStudyById(Long id) {
        Study study = studyAccessValidator.getStudyOrThrow(id);
        int memberCount = studyMemberRepository.countByStudyId(id);
        List<String> imageUrls = getImageUrls(id);

        return StudyDetailResponse.from(study, memberCount, imageUrls);
    }

    @Transactional
    public StudyResponse updateStudy(Long userId, Long id, @Valid StudyUpdateRequest request) {
        Study study = studyAccessValidator.getStudyOrThrow(id);
        studyAccessValidator.validateStudyLeader(study, userId);

        int memberCount = studyMemberRepository.countByStudyId(id);
        if (request.getCapacity() < memberCount) {
            throw new BusinessException(StudyErrorCode.STUDY_CAPACITY_BELOW_CURRENT_MEMBERS);
        }

        study.setTitle(request.getTitle());
        study.setDescription(request.getDescription());
        study.setCapacity(request.getCapacity());
        study.setRecruitEnd(request.getRecruitEnd());
        study.setCategory(request.getCategory());

        List<String> imageUrls = getImageUrls(id);

        return StudyResponse.from(study, memberCount, imageUrls);
    }

    @Transactional
    public void deleteStudy(Long userId, Long id) {
        Study study = studyAccessValidator.getStudyOrThrow(id);
        studyAccessValidator.validateStudyLeader(study, userId);

        deleteStudyCascade(study);
    }

    @Transactional
    public void adminDeleteStudy(Long studyId) {
        Study study = studyAccessValidator.getStudyOrThrow(studyId);
        deleteStudyCascade(study);
    }

    @Transactional
    public void deleteStudyCascade(Study study) {
        studyPostService.deleteAllByStudy(study.getId());   // 원래 없던 게시글/댓글 cascade — 버그 수정
        studyMemberRepository.deleteByStudyId(study.getId());
        studyImageRepository.deleteAllByStudyId(study.getId());
        studyRepository.delete(study);
    }

    public Page<StudyResponse> getMyStudies(Long userId, Pageable pageable) {
        Page<StudyMember> members = studyMemberRepository.findByUserId(userId, pageable);

        List<Study> studies = members.getContent().stream()
                .map(StudyMember::getStudy)
                .toList();
        Map<Long, Integer> memberCountMap = getMemberCountMap(studies);

        return members.map(member -> {
            Study study = member.getStudy();
            int memberCount = memberCountMap.getOrDefault(study.getId(), 0);
            List<String> imageUrls = getImageUrls(study.getId());
            return StudyResponse.from(study, memberCount, imageUrls);
        });
    }

    private List<String> saveStudyImages(Study study, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<String> imageUrls = new java.util.ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            imageValidator.validate(file);
            String url = fileStorageService.upload(file, "study");
            studyImageRepository.save(new StudyImage(study, url, file.getOriginalFilename(), i));
            imageUrls.add(url);
        }
        return imageUrls;
    }

    private List<String> getImageUrls(Long studyId) {
        return studyImageRepository.findAllByStudyIdOrderByImageOrder(studyId).stream()
                .map(StudyImage::getImageUrl)
                .toList();
    }

    private Map<Long, Integer> getMemberCountMap(List<Study> studies) {
        List<Long> studyIds = studies.stream().map(Study::getId).toList();

        if (studyIds.isEmpty()) {
            return Map.of();
        }

        return studyMemberRepository.countByStudyIds(studyIds).stream()
                .collect(Collectors.toMap(
                        StudyMemberRepository.StudyMemberCountProjection::getStudyId,
                        p -> p.getMemberCount().intValue()
                ));
    }

}