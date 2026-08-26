package org.example.backend.study.service;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.dto.response.StudyMemberResponse;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyCategory;
import org.example.backend.study.entity.StudyMember;
import org.example.backend.study.exception.StudyErrorCode;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyMemberServiceTest {

    @Mock
    private StudyMemberRepository studyMemberRepository;
    @Mock
    private StudyAccessValidator studyAccessValidator;
    @Mock
    private StudyService studyService;
    @InjectMocks
    private StudyMemberService studyMemberService;

    private User leader;
    private User applicant;
    private Study study;

    @BeforeEach
    void setUp() {
        leader = new User();
        leader.setId(1L);
        leader.setRole(Role.USER);

        applicant = new User();
        applicant.setId(2L);
        applicant.setRole(Role.USER);
        applicant.setSubscribed(false);

        study = new Study("알고리즘 스터디", "설명", 2, LocalDate.now(), null, leader, StudyCategory.IT_DEVELOPMENT);
    }

    // 동시 가입 요청이 같은 인원 수를 읽고 정원을 초과해 저장하는 것을 막기 위해
    // getStudyOrThrow(락 없음)가 아니라 getStudyForUpdateOrThrow(락 있음)를 거치는지 확인한다.
    @Test
    void joinStudy_정상가입시_락걸린스터디조회를사용한다() {
        when(studyAccessValidator.getUserOrThrow(2L)).thenReturn(applicant);
        when(studyAccessValidator.getStudyForUpdateOrThrow(1L)).thenReturn(study);
        when(studyMemberRepository.findByStudyIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(studyMemberRepository.countByStudyId(1L)).thenReturn(0);
        when(studyMemberRepository.save(any(StudyMember.class))).thenAnswer(inv -> inv.getArgument(0));

        StudyMemberResponse response = studyMemberService.joinStudy(2L, 1L);

        assertThat(response.getUserId()).isEqualTo(2L);
        verify(studyAccessValidator).getStudyForUpdateOrThrow(1L);
        verify(studyAccessValidator, never()).getStudyOrThrow(any());
    }

    // 락을 걸고 다시 읽은 인원 수 기준으로 정원을 판단해야, 락 대기 중 다른 요청이 먼저 채운
    // 자리를 뒤늦게 또 채우는 일이 없다.
    @Test
    void joinStudy_락걸린조회로읽은인원수가정원과같으면_예외() {
        when(studyAccessValidator.getUserOrThrow(2L)).thenReturn(applicant);
        when(studyAccessValidator.getStudyForUpdateOrThrow(1L)).thenReturn(study);
        when(studyMemberRepository.findByStudyIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(studyMemberRepository.countByStudyId(1L)).thenReturn(2); // capacity와 동일 = 이미 만석

        BusinessException e = assertThrows(BusinessException.class,
                () -> studyMemberService.joinStudy(2L, 1L));

        assertThat(e.getErrorCode()).isEqualTo(StudyErrorCode.STUDY_CAPACITY_EXCEEDED);
        verify(studyMemberRepository, never()).save(any());
    }

    @Test
    void joinStudy_존재하지않는스터디면_예외() {
        when(studyAccessValidator.getUserOrThrow(2L)).thenReturn(applicant);
        when(studyAccessValidator.getStudyForUpdateOrThrow(eq(999L)))
                .thenThrow(new BusinessException(StudyErrorCode.STUDY_NOT_FOUND));

        BusinessException e = assertThrows(BusinessException.class,
                () -> studyMemberService.joinStudy(2L, 999L));

        assertThat(e.getErrorCode()).isEqualTo(StudyErrorCode.STUDY_NOT_FOUND);
        verify(studyMemberRepository, never()).save(any());
    }
}
