package org.example.backend.study.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.exception.AuthErrorCode;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.dto.request.StudyRequest;
import org.example.backend.study.dto.request.StudyUpdateRequest;
import org.example.backend.study.dto.response.StudyDetailResponse;
import org.example.backend.study.dto.response.StudyMemberResponse;
import org.example.backend.study.dto.response.StudyResponse;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyMember;
import org.example.backend.study.exception.*;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;

    public StudyResponse createStudy(Long userId, StudyRequest request){
        User user = getUserOrThrow(userId);
        validateFreeTierLimit(user);

        Study study = new Study(request.getTitle(), request.getDescription(), request.getCapacity(), request.getRecruitStart(), request.getRecruitEnd(), user, request.getCategory());
        Study saved = studyRepository.save(study);

        studyMemberRepository.save(new StudyMember(saved, user));

        return StudyResponse.from(saved);
    }

    public Page<StudyResponse> getStudies(String keyword, Pageable pageable) {
        Page<Study> page = (keyword == null || keyword.isBlank())
                ? studyRepository.findAll(pageable)
                : studyRepository.findByTitleContaining(keyword, pageable);

        return page.map(StudyResponse::from);
    }

    public StudyDetailResponse getStudyById(Long id) {
        Study study = getStudyOrThrow(id);
        int memberCount = studyMemberRepository.countByStudyId(id);

        return StudyDetailResponse.from(study, memberCount);
    }

    public StudyResponse updateStudy(Long userId, Long id, @Valid StudyUpdateRequest request) {
        Study study = getStudyOrThrow(id);
        validateStudyLeader(study, userId);

        study.setTitle(request.getTitle());
        study.setDescription(request.getDescription());
        study.setCapacity(request.getCapacity());
        study.setRecruitStart(request.getRecruitStart());
        study.setRecruitEnd(request.getRecruitEnd());
        study.setCategory(request.getCategory());

        return StudyResponse.from(study);
    }

    public void deleteStudy(Long userId, Long id) {
        Study study = getStudyOrThrow(id);
        validateStudyLeader(study, userId);

        studyRepository.delete(study);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    private Study getStudyOrThrow(Long id) {
        return studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    private void validateStudyLeader(Study study, Long userId) {
        if (!study.getLeader().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_ACCESS_DENIED);
        }
    }

    private void validateFreeTierLimit(User user) {
        if (!user.isSubscribed()) {
            int joinedStudyCount = studyMemberRepository.countByUserId(user.getId());
            if (joinedStudyCount >= 2) {
                throw new BusinessException(StudyErrorCode.STUDY_ALREADY_JOINED);
            }
        }
    }

    public Page<StudyResponse> getMyStudies(Long userId, Pageable pageable) {
        Page<StudyMember> members = studyMemberRepository.findByUserId(userId, pageable);
        return members.map(member -> StudyResponse.from(member.getStudy()));
    }
}
