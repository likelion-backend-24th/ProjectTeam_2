package org.example.backend.expert.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.expert.dto.ExpertProfileResponse;
import org.example.backend.expert.dto.ExpertSignupRequest;
import org.example.backend.expert.dto.ExpertListResponse;
import org.example.backend.expert.dto.ExpertSignupResponse;
import org.example.backend.expert.dto.PublicExpertListResponse;
import org.example.backend.expert.dto.PublicExpertResponse;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.expert.exception.ExpertErrorCode;
import org.example.backend.expert.repository.ExpertProfileRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 1. signup(userId, request)
 * — 전문가 신청 처리. 기존 프로필 있으면 재신청(REJECTED→PENDING), 없으면 신규 생성.
 *
 * 2. approve(expertProfileId)
 * — 관리자가 신청 승인. 상태를 APPROVED로, 유저 role을 EXPERT로 변경.
 *
 * 3. reject(expertProfileId, reason)
 * — 관리자가 신청 거절. 상태를 REJECTED로, 사유 저장.
 *
 * 4. getList(status)
 * — 관리자용 전문가 목록 조회. status 없으면 전체, 있으면 필터링.
 *
 * 5. revoke(expertProfileId, reason)
 * — 이미 승인된 전문가의 자격 박탈. 상태를 REJECTED로, 유저 role을 USER로 되돌림.
 *
 * 6. getPublicList()
 * — 비로그인 사용자도 볼 수 있는 공개 전문가 목록(APPROVED만) 조회.
 *
 * 7. getProfileOrThrow(id)
 * — id로 프로필 조회, 없으면 404 예외 (내부 공용 헬퍼).
 * → approve, reject, revoke에서 id를 찾고 없으면 404 로직 한 곳에 정리.
 *
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertProfileService {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExpertSignupResponse signup(Long userId, ExpertSignupRequest request) {
        return expertProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.reapply(request.getCareer(), request.getCertification());
                    return ExpertSignupResponse.from(profile);
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ExpertErrorCode.USER_NOT_FOUND));
                    ExpertProfile profile = ExpertProfile.builder()
                            .user(user)
                            .career(request.getCareer())
                            .certification(request.getCertification())
                            .build();
                    expertProfileRepository.save(profile);
                    return ExpertSignupResponse.from(profile);
                });
    }

    @Transactional
    public ExpertProfileResponse approve(Long expertProfileId) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.approve();
        profile.getUser().setRole(Role.EXPERT);
        return ExpertProfileResponse.from(profile);
    }


    @Transactional
    public ExpertProfileResponse reject(Long expertProfileId, String reason) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.reject(reason);
        return ExpertProfileResponse.from(profile);
    }

    public ExpertListResponse getList(ExpertStatus status) {
        List<ExpertProfile> profiles = (status == null)
                ? expertProfileRepository.findAll()
                : expertProfileRepository.findByStatus(status);
        List<ExpertProfileResponse> responses = profiles.stream().map(ExpertProfileResponse::from).toList();
        return ExpertListResponse.from(responses);
    }

    @Transactional
    public ExpertProfileResponse revoke(Long expertProfileId, String reason) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.revoke(reason);
        profile.getUser().setRole(Role.USER);
        return ExpertProfileResponse.from(profile);
    }

    public PublicExpertListResponse getPublicList() {
        List<PublicExpertResponse> responses = expertProfileRepository.findByStatus(ExpertStatus.APPROVED)
                .stream().map(PublicExpertResponse::from).toList();
        return PublicExpertListResponse.from(responses);
    }

    // id로 ExpertProfile을 조회하고, 없으면 BusinessException(EXPERT_PROFILE_NOT_FOUND, 404)을 던지는 공용 헬퍼.
    private ExpertProfile getProfileOrThrow(Long id) {
        return expertProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
    }
}