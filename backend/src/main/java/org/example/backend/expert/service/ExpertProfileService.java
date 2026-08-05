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