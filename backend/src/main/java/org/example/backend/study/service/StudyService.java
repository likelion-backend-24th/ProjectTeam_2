package org.example.backend.study.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.dto.*;
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
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if (!user.isSubscribed()) {
            int joinedStudyCount = studyMemberRepository.countByUserId(userId);
            if (joinedStudyCount >= 2) {
                throw new BusinessException(StudyErrorCode.STUDY_ALREADY_JOINED);
            }
        }

        Study study = new Study(request.getTitle(), request.getDescription(), request.getCapacity(), request.getRecruitStart(), request.getRecruitEnd(), user);
        Study saved = studyRepository.save(study);

        StudyMember leaderAsMember = new StudyMember(saved, user);
        studyMemberRepository.save(leaderAsMember);

        return StudyResponse.from(saved);
    }

    public Page<StudyResponse> getStudies(String keyword, Pageable pageable) {
        Page<Study> page = (keyword == null || keyword.isBlank())
                ? studyRepository.findAll(pageable)
                : studyRepository.findByTitleContaining(keyword, pageable);

        return page.map(StudyResponse::from);
    }

    public StudyDetailResponse getStudyById(Long id) {
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
        List<StudyMemberResponse> members = studyMemberRepository.findByStudyId(id).stream()
                .map(StudyMemberResponse::from)
                .toList();

        return StudyDetailResponse.from(study, members);
    }

    public StudyResponse updateStudy(Long userId, Long id, @Valid StudyUpdateRequest request) {
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
        if (!study.getLeader().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_ACCESS_DENIED);
        }

        study.setTitle(request.getTitle());
        study.setDescription(request.getDescription());
        study.setCapacity(request.getCapacity());
        study.setRecruitStart(request.getRecruitStart());
        study.setRecruitEnd(request.getRecruitEnd());

        Study updated = studyRepository.save(study);
        return StudyResponse.from(updated);
    }


    public void deleteStudy(Long userId, Long id) {
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));

        if (!study.getLeader().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_ACCESS_DENIED);
        }

        studyRepository.delete(study);
    }

    public StudyMemberResponse joinStudy(Long userId, Long id) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Study study = studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));

        if (study.getLeader().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_LEADER_CANNOT_JOIN);
        }

        studyMemberRepository.findByStudyIdAndUserId(id, userId)
                .ifPresent(member -> {
                    throw new BusinessException(StudyErrorCode.STUDY_ALREADY_JOINED);
                });

        int currentCount = studyMemberRepository.countByStudyId(id);
        if (currentCount >= study.getCapacity()) {
            throw new BusinessException(StudyErrorCode.STUDY_CAPACITY_EXCEEDED);
        }

        if (!user.isSubscribed()) {
            int joinedStudyCount = studyMemberRepository.countByUserId(userId);
            if (joinedStudyCount >= 2) {
                throw new BusinessException(StudyErrorCode.STUDY_JOIN_LIMIT_EXCEEDED);
            }
        }

        StudyMember member = new StudyMember(study, user);
        StudyMember saved = studyMemberRepository.save(member);

        return StudyMemberResponse.from(saved);
    }
}
