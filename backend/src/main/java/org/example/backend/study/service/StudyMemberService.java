package org.example.backend.study.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.exception.AuthErrorCode;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.dto.response.StudyMemberResponse;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyMember;
import org.example.backend.study.exception.StudyErrorCode;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyMemberService {

    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;

    public StudyMemberResponse joinStudy(Long userId, Long id) {
        User user = getUserOrThrow(userId);
        Study study = getStudyOrThrow(id);

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

        validateFreeTierLimit(user);

        StudyMember member = new StudyMember(study, user);
        StudyMember saved = studyMemberRepository.save(member);

        return StudyMemberResponse.from(saved);
    }

    public List<StudyMemberResponse> getStudyMembers(Long id) {
        getStudyOrThrow(id);
        List<StudyMember> members = studyMemberRepository.findByStudyId(id);
        return members.stream()
                .map(StudyMemberResponse::from)
                .toList();
    }

    public void removeStudyMember(Long userId, Long id, Long memberId) {
        getUserOrThrow(userId);
        Study study = getStudyOrThrow(id);
        validateStudyLeader(study, userId);
        StudyMember member = getStudyMemberOrThrow(id, memberId);

        if (member.getUser().getId().equals(study.getLeader().getId())) {
            throw new BusinessException(StudyErrorCode.STUDY_LEADER_CANNOT_BE_REMOVED);
        }
        studyMemberRepository.delete(member);
    }

    public void delegateLeader(Long userId, Long id, Long newLeaderId) {
        getUserOrThrow(userId);
        Study study = getStudyOrThrow(id);
        validateStudyLeader(study, userId);

        if (newLeaderId.equals(userId)) {
            throw new BusinessException(StudyErrorCode.STUDY_LEADER_DELEGATE_SELF);
        }

        StudyMember newLeaderMember = getStudyMemberOrThrow(id, newLeaderId);

        study.setLeader(newLeaderMember.getUser());
    }

    public void leaveStudy(Long userId, Long id) {
        getUserOrThrow(userId);
        Study study = getStudyOrThrow(id);

        if (study.getLeader().getId().equals(userId)) {
            Long memberCount = studyRepository.countByStudyId(id);
            if (memberCount <= 1) {
                studyMemberRepository.deleteByStudyId(id);
                studyRepository.deleteById(id);
            } else {
                throw new BusinessException(StudyErrorCode.STUDY_LEADER_MUST_DELEGATE);
            }
            return;
        }

        StudyMember member = getStudyMemberOrThrow(id, userId);
        studyMemberRepository.delete(member);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    private Study getStudyOrThrow(Long id) {
        return studyRepository.findById(id).orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));
    }

    private StudyMember getStudyMemberOrThrow(Long id, Long memberId) {
        return studyMemberRepository.findByStudyIdAndUserId(id, memberId)
                .orElseThrow(() -> new BusinessException(StudyErrorCode.STUDY_MEMBER_NOT_FOUND));
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
}
