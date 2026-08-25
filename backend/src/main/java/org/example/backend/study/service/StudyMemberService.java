package org.example.backend.study.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.dto.response.StudyMemberResponse;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyMember;
import org.example.backend.study.exception.StudyErrorCode;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyMemberService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyAccessValidator studyAccessValidator;
    private final StudyService studyService;

    @Transactional
    public StudyMemberResponse joinStudy(Long userId, Long id) {
        User user = studyAccessValidator.getUserOrThrow(userId);
        Study study = studyAccessValidator.getStudyOrThrow(id);

        if (study.getLeader().getId().equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_LEADER_CANNOT_JOIN);
        }

        studyMemberRepository.findByStudyIdAndUserId(id, userId)
                .ifPresent(member -> {
                    throw new BusinessException(StudyErrorCode.STUDY_ALREADY_JOINED);
                });

        if (study.getRecruitEnd() != null && study.getRecruitEnd().isBefore(LocalDate.now())) {
            throw new BusinessException(StudyErrorCode.STUDY_RECRUIT_CLOSED);
        }

        int currentCount = studyMemberRepository.countByStudyId(id);
        if (currentCount >= study.getCapacity()) {
            throw new BusinessException(StudyErrorCode.STUDY_CAPACITY_EXCEEDED);
        }

        studyAccessValidator.validateFreeTierLimit(user);

        StudyMember member = new StudyMember(study, user);
        StudyMember saved = studyMemberRepository.save(member);

        return StudyMemberResponse.from(saved);
    }

    public List<StudyMemberResponse> getStudyMembers(Long id) {
        studyAccessValidator.getStudyOrThrow(id);
        List<StudyMember> members = studyMemberRepository.findByStudyId(id);
        return members.stream()
                .map(StudyMemberResponse::from)
                .toList();
    }

    @Transactional
    public void removeStudyMember(Long userId, Long id, Long memberId) {
        studyAccessValidator.getUserOrThrow(userId);
        Study study = studyAccessValidator.getStudyOrThrow(id);
        studyAccessValidator.validateStudyLeader(study, userId);
        StudyMember member = getStudyMemberOrThrow(id, memberId);

        if (member.getUser().getId().equals(study.getLeader().getId())) {
            throw new BusinessException(StudyErrorCode.STUDY_LEADER_CANNOT_BE_REMOVED);
        }
        studyMemberRepository.delete(member);
    }

    @Transactional
    public void delegateLeader(Long userId, Long id, Long newLeaderId) {
        studyAccessValidator.getUserOrThrow(userId);
        Study study = studyAccessValidator.getStudyOrThrow(id);
        studyAccessValidator.validateStudyLeader(study, userId);

        if (newLeaderId.equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_LEADER_DELEGATE_SELF);
        }

        StudyMember newLeaderMember = getStudyMemberOrThrow(id, newLeaderId);

        study.setLeader(newLeaderMember.getUser());
    }

    @Transactional
    public void leaveStudy(Long userId, Long id) {
        studyAccessValidator.getUserOrThrow(userId);
        Study study = studyAccessValidator.getStudyOrThrow(id);

        if (study.getLeader().getId().equals(userId)) {
            int memberCount = studyMemberRepository.countByStudyId(id);
            if (memberCount <= 1) {
                studyService.deleteStudyCascade(study);
            } else {
                throw new BusinessException(StudyErrorCode.STUDY_LEADER_MUST_DELEGATE);
            }
            return;
        }

        StudyMember member = getStudyMemberOrThrow(id, userId);
        studyMemberRepository.delete(member);
    }

    private StudyMember getStudyMemberOrThrow(Long id, Long memberId) {
        return studyMemberRepository.findByStudyIdAndUserId(id, memberId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
    }

}
