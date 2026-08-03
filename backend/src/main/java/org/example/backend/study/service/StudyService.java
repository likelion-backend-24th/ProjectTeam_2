package org.example.backend.study.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
                throw new StudyJoinLimitExceededException();
            }
        }

        Study study = new Study(request.getTitle(), request.getDescription(), request.getCapacity(), request.getRecruitStart(), request.getRecruitEnd(), user);
        Study saved = studyRepository.save(study);

        // 방장을 멤버로도 등록 (그룹 개설 -> 방장이 첫 멤버로 자동 가입되는 구조)
        StudyMember leaderAsMember = new StudyMember(saved, user);
        studyMemberRepository.save(leaderAsMember);

        return StudyResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .capacity(saved.getCapacity())
                .recruitStart(saved.getRecruitStart())
                .recruitEnd(saved.getRecruitEnd())
                .leaderId(user.getId())
                .leaderNickname(user.getNickname())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public Page<StudyResponse> getStudies(String keyword, Pageable pageable) {
        Page<Study> page = (keyword == null || keyword.isBlank())
                ? studyRepository.findAll(pageable)
                : studyRepository.findByTitleContaining(keyword, pageable);

        return page.map(study -> StudyResponse.builder()
                .id(study.getId())
                .title(study.getTitle())
                .description(study.getDescription())
                .capacity(study.getCapacity())
                .recruitStart(study.getRecruitStart())
                .recruitEnd(study.getRecruitEnd())
                .leaderId(study.getLeader().getId())
                .leaderNickname(study.getLeader().getNickname())
                .createdAt(study.getCreatedAt())
                .build());
    }

    public StudyDetailResponse getStudyById(Long id) {
        Study study = studyRepository.findById(id).orElseThrow(StudyNotFoundException::new);
        List<StudyMember> members = studyMemberRepository.findByStudyId(id);

        List<StudyMemberResponse> memberResponses = members.stream()
                .map(member -> StudyMemberResponse.builder()
                        .memberId(member.getId())
                        .userId(member.getUser().getId())
                        .nickname(member.getUser().getNickname())
                        .joinedAt(member.getJoinedAt())
                        .build())
                .toList();

        return StudyDetailResponse.builder()
                .id(study.getId())
                .title(study.getTitle())
                .description(study.getDescription())
                .capacity(study.getCapacity())
                .currentMemberCount(memberResponses.size())
                .recruitStart(study.getRecruitStart())
                .recruitEnd(study.getRecruitEnd())
                .leaderId(study.getLeader().getId())
                .leaderNickname(study.getLeader().getNickname())
                .createdAt(study.getCreatedAt())
                .members(memberResponses)
                .build();
    }

    public StudyResponse updateStudy(Long userId, Long id, @Valid StudyUpdateRequest request) {
        Study study = studyRepository.findById(id).orElseThrow(StudyNotFoundException::new);
        if (!study.getLeader().getId().equals(userId)) {
            throw new StudyAccessDeniedException();
        }

        study.setTitle(request.getTitle());
        study.setDescription(request.getDescription());
        study.setCapacity(request.getCapacity());
        study.setRecruitStart(request.getRecruitStart());
        study.setRecruitEnd(request.getRecruitEnd());

        Study updated = studyRepository.save(study);
        return StudyResponse.builder()
                .id(updated.getId())
                .title(updated.getTitle())
                .description(updated.getDescription())
                .capacity(updated.getCapacity())
                .recruitStart(updated.getRecruitStart())
                .recruitEnd(updated.getRecruitEnd())
                .leaderId(updated.getLeader().getId())
                .leaderNickname(updated.getLeader().getNickname())
                .createdAt(updated.getCreatedAt())
                .build();
    }


    public void deleteStudy(Long userId, Long id) {
        Study study = studyRepository.findById(id).orElseThrow(StudyNotFoundException::new);

        // 방장이 아니면 예외
        if (!study.getLeader().getId().equals(userId)) {
            throw new StudyAccessDeniedException();
        }

        studyRepository.delete(study);
    }

    public StudyMemberResponse joinStudy(Long userId, Long id) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Study study = studyRepository.findById(id).orElseThrow(StudyNotFoundException::new);

        // 가입 신청인이 방장이면 예외
        if (study.getLeader().getId().equals(userId)) {
            throw new StudyLeaderCannotJoinException();
        }

        // 해당 유저가 이미 그 그룹에 속해있는지 확인
        // ifPresent - Optional 객체에서 null이 발생하면 실행되지 않도록 함 (여기서는 null이면 통과, null이 아니면 예외)
        studyMemberRepository.findByStudyIdAndUserId(id, userId)
                .ifPresent(member -> {
                    throw new StudyAlreadyJoinedException();
                });

        // 모집 인원 수 확인
        int currentCount = studyMemberRepository.countByStudyId(id);
        if (currentCount >= study.getCapacity()) {
            throw new StudyCapacityExceededException();
        }

        // 비구독자 2개 제한 검증
        if (!user.isSubscribed()) {
            int joinedStudyCount = studyMemberRepository.countByUserId(userId);
            if (joinedStudyCount >= 2) {
                throw new StudyJoinLimitExceededException();
            }
        }

        // 모든 조건 통과 후 가입
        StudyMember member = new StudyMember(study, user);
        StudyMember saved = studyMemberRepository.save(member);

        return StudyMemberResponse.builder()
                .memberId(saved.getId())
                .userId(userId)
                .nickname(user.getNickname())
                .joinedAt(saved.getJoinedAt())
                .build();
    }
}
