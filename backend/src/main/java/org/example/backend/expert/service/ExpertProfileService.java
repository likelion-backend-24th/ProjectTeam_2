package org.example.backend.expert.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.expert.dto.ExpertProfileResponse;
import org.example.backend.expert.dto.ExpertSignupRequest;
import org.example.backend.expert.dto.ExpertListResponse;
import org.example.backend.expert.dto.ExpertSignupResponse;
import org.example.backend.expert.dto.PublicExpertListResponse;
import org.example.backend.expert.dto.PublicExpertResponse;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.expert.exception.ExpertProfileNotFoundException;
import org.example.backend.expert.repository.ExpertProfileRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 전문가 신청/심사/공개조회를 처리하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertProfileService {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;

    /**
     * F-25: 전문가 신청 처리.
     * userId로 기존 프로필이 있는지 먼저 조회하고,
     * - 있으면 ExpertProfile.reapply()를 호출해 재신청으로 처리한다(REJECTED 상태에서만 성공,
     *   그 외 상태면 ExpertProfile 쪽에서 InvalidExpertStatusException을 던진다).
     * - 없으면 신규 ExpertProfile을 만들어 저장한다(생성자에서 상태가 자동으로 PENDING이 됨).
     * 두 경로 모두 최종적으로 ExpertSignupResponse{expertId, status}를 반환한다.
     */
    @Transactional
    public ExpertSignupResponse signup(Long userId, ExpertSignupRequest request) {
        return expertProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.reapply(request.getCareer(), request.getCertification());
                    return ExpertSignupResponse.from(profile);
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
                    ExpertProfile profile = ExpertProfile.builder()
                            .user(user)
                            .career(request.getCareer())
                            .certification(request.getCertification())
                            .build();
                    expertProfileRepository.save(profile);
                    return ExpertSignupResponse.from(profile);
                });
    }

    /**
     * F-26: 관리자가 신청을 승인한다.
     * ExpertProfile.approve()로 상태를 APPROVED로 바꾸고, 이어서 User.role을 EXPERT로
     * 바꿔서 두 엔티티의 상태를 함께 갱신한다(하나의 트랜잭션 안에서 처리되므로 원자적으로 반영됨).
     */
    @Transactional
    public ExpertProfileResponse approve(Long expertProfileId) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.approve();
        profile.getUser().setRole(Role.EXPERT);
        return ExpertProfileResponse.from(profile);
    }

    /**
     * F-26: 관리자가 신청을 거절한다. User.role은 아직 EXPERT로 바뀐 적이 없으므로
     * 별도로 되돌릴 필요가 없다(approve() 이전 단계이기 때문).
     */
    @Transactional
    public ExpertProfileResponse reject(Long expertProfileId, String reason) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.reject(reason);
        return ExpertProfileResponse.from(profile);
    }

    /**
     * F-27: 관리자용 전문가 목록 조회. status 파라미터가 없으면 전체(findAll),
     * 있으면 해당 상태만(findByStatus) 조회해서 ExpertProfileResponse 리스트로 변환한다.
     */
    public ExpertListResponse getList(ExpertStatus status) {
        List<ExpertProfile> profiles = (status == null)
                ? expertProfileRepository.findAll()
                : expertProfileRepository.findByStatus(status);
        List<ExpertProfileResponse> responses = profiles.stream().map(ExpertProfileResponse::from).toList();
        return ExpertListResponse.from(responses);
    }

    /**
     * F-27: 관리자가 이미 승인된 전문가의 자격을 박탈한다.
     * ExpertProfile.revoke()로 상태를 REJECTED로 되돌리고, User.role도 다시 USER로
     * 되돌려서 approve()에서 바뀐 두 상태를 원래대로 복원한다.
     */
    @Transactional
    public ExpertProfileResponse revoke(Long expertProfileId, String reason) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.revoke(reason);
        profile.getUser().setRole(Role.USER);
        return ExpertProfileResponse.from(profile);
    }

    /**
     * F-32: 비로그인 사용자도 볼 수 있는 공개 전문가 목록. status가 APPROVED인
     * 프로필만 골라서 PublicExpertResponse(닉네임·경력·자격증만 노출)로 변환한다.
     */
    public PublicExpertListResponse getPublicList() {
        List<PublicExpertResponse> responses = expertProfileRepository.findByStatus(ExpertStatus.APPROVED)
                .stream().map(PublicExpertResponse::from).toList();
        return PublicExpertListResponse.from(responses);
    }

    // id로 ExpertProfile을 조회하고, 없으면 ExpertProfileNotFoundException(404)을 던지는 공용 헬퍼.
    private ExpertProfile getProfileOrThrow(Long id) {
        return expertProfileRepository.findById(id)
                .orElseThrow(() -> new ExpertProfileNotFoundException("존재하지 않는 전문가 프로필입니다."));
    }
}