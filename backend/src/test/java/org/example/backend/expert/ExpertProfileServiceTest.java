package org.example.backend.expert.service;

import org.example.backend.expert.dto.ExpertListResponse;
import org.example.backend.expert.dto.ExpertProfileResponse;
import org.example.backend.expert.dto.ExpertSignupRequest;
import org.example.backend.expert.dto.ExpertSignupResponse;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.expert.exception.ExpertProfileNotFoundException;
import org.example.backend.expert.exception.InvalidExpertStatusException;
import org.example.backend.expert.repository.ExpertProfileRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpertProfileServiceTest {

    @Mock
    private ExpertProfileRepository expertProfileRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExpertProfileService expertProfileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("test@test.com");
        user.setNickname("테스터");
        user.setRole(Role.USER);
    }

    @Test
    void signup_기존신청이PENDING이면_예외() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("3년").certification("정보처리기사").build();
        when(expertProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertThrows(InvalidExpertStatusException.class,
                () -> expertProfileService.signup(1L, new ExpertSignupRequest()));

        verify(userRepository, never()).findById(any());
    }

    @Test
    void signup_기존신청이APPROVED면_예외() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("3년").certification("정보처리기사").build();
        profile.approve();
        when(expertProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertThrows(InvalidExpertStatusException.class,
                () -> expertProfileService.signup(1L, new ExpertSignupRequest()));
    }

    @Test
    void signup_처음신청이면_PENDING상태의_전문가프로필_생성() {
        when(expertProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ExpertSignupRequest request = new ExpertSignupRequest();
        request.setCareer("3년");
        request.setCertification("정보처리기사");

        ExpertSignupResponse response = expertProfileService.signup(1L, request);

        assertThat(response.getStatus()).isEqualTo(ExpertStatus.PENDING);
        verify(expertProfileRepository).save(any(ExpertProfile.class));
    }

    @Test
    void signup_기존신청이REJECTED면_재신청으로_PENDING전환() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("1년").certification(null).build();
        profile.reject("경력 부족");
        when(expertProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        ExpertSignupRequest request = new ExpertSignupRequest();
        request.setCareer("3년");
        request.setCertification("정보처리기사");

        ExpertSignupResponse response = expertProfileService.signup(1L, request);

        assertThat(response.getStatus()).isEqualTo(ExpertStatus.PENDING);
        assertThat(profile.getCareer()).isEqualTo("3년");
        assertThat(profile.getRejectReason()).isNull();
        verify(expertProfileRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void approve_승인시_상태APPROVED_역할EXPERT로_변경() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("3년").certification("정보처리기사").build();
        when(expertProfileRepository.findById(10L)).thenReturn(Optional.of(profile));

        ExpertProfileResponse response = expertProfileService.approve(10L);

        assertThat(response.getStatus()).isEqualTo(ExpertStatus.APPROVED);
        assertThat(user.getRole()).isEqualTo(Role.EXPERT);
    }

    @Test
    void reject_거절시_사유가_저장됨() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("1년").certification(null).build();
        when(expertProfileRepository.findById(5L)).thenReturn(Optional.of(profile));

        ExpertProfileResponse response = expertProfileService.reject(5L, "경력 부족");

        assertThat(response.getStatus()).isEqualTo(ExpertStatus.REJECTED);
        assertThat(response.getRejectReason()).isEqualTo("경력 부족");
    }

    @Test
    void revoke_자격박탈시_상태REJECTED_역할USER로_원복() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("5년").certification("정보처리기사").build();
        profile.approve();
        user.setRole(Role.EXPERT);
        when(expertProfileRepository.findById(7L)).thenReturn(Optional.of(profile));

        ExpertProfileResponse response = expertProfileService.revoke(7L, "허위 경력");

        assertThat(response.getStatus()).isEqualTo(ExpertStatus.REJECTED);
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void approve_이미승인된대상이면_예외() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("3년").certification("정보처리기사").build();
        profile.approve();
        when(expertProfileRepository.findById(11L)).thenReturn(Optional.of(profile));

        assertThrows(InvalidExpertStatusException.class, () -> expertProfileService.approve(11L));
    }

    @Test
    void reject_이미거절된대상이면_예외() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("1년").certification(null).build();
        profile.reject("경력 부족");
        when(expertProfileRepository.findById(12L)).thenReturn(Optional.of(profile));

        assertThrows(InvalidExpertStatusException.class, () -> expertProfileService.reject(12L, "재검토 결과 거절"));
    }

    @Test
    void revoke_PENDING상태면_예외() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("3년").certification("정보처리기사").build();
        when(expertProfileRepository.findById(13L)).thenReturn(Optional.of(profile));

        assertThrows(InvalidExpertStatusException.class, () -> expertProfileService.revoke(13L, "사유"));
    }

    @Test
    void approve_없는id면_ExpertProfileNotFoundException() {
        when(expertProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ExpertProfileNotFoundException.class, () -> expertProfileService.approve(999L));
    }

    @Test
    void getList_상태null이면_findAll_호출하고_experts로_감싸서_반환() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("5년").certification("정보처리기사").build();
        when(expertProfileRepository.findAll()).thenReturn(List.of(profile));

        ExpertListResponse response = expertProfileService.getList(null);

        assertThat(response.getExperts()).hasSize(1);
        verify(expertProfileRepository).findAll();
        verify(expertProfileRepository, never()).findByStatus(any());
    }

    @Test
    void getPublicList_APPROVED만_조회() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user).career("5년").certification("정보처리기사").build();
        profile.approve();
        when(expertProfileRepository.findByStatus(ExpertStatus.APPROVED)).thenReturn(List.of(profile));

        var response = expertProfileService.getPublicList();

        assertThat(response.getExperts()).hasSize(1);
        assertThat(response.getExperts().get(0).getNickname()).isEqualTo("테스터");
        assertThat(response.getExperts().get(0).getCareer()).isEqualTo("5년");
    }
}