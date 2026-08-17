package org.example.backend.expert.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.expert.dto.response.*;
import org.example.backend.expert.dto.request.ExpertSignupRequest;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.expert.exception.ExpertErrorCode;
import org.example.backend.expert.repository.ExpertProfileRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.backend.expert.entity.Career;
import org.example.backend.expert.entity.Certification;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertProfileService {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final FeedbackService feedbackService;

    @Transactional
    public ExpertSignupResponse signup(Long userId, ExpertSignupRequest request) {
        return expertProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.reapply();
                    profile.updateIntroduction(request.getIntroduction());
                    applyCareersAndCertifications(profile, request);
                    // dirty checking으로 자동 update 수행
                    return ExpertSignupResponse.from(profile);
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ExpertErrorCode.USER_NOT_FOUND));
                    ExpertProfile profile = ExpertProfile.builder()
                            .user(user)
                            .introduction(request.getIntroduction())
                            .build();
                    applyCareersAndCertifications(profile, request);
                    expertProfileRepository.save(profile); // 명시적 save() 표시
                    return ExpertSignupResponse.from(profile);
                });
    }

    @Transactional
    public ExpertProfileResponse approve(Long expertProfileId) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.approve();
        profile.getUser().setRole(Role.EXPERT);
        // dirty checking
        return ExpertProfileResponse.from(profile);
    }

    @Transactional
    public ExpertProfileResponse reject(Long expertProfileId, String reason) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.reject(reason);
        return ExpertProfileResponse.from(profile);
    }

    public Page<ExpertProfileResponse> getList(ExpertStatus status, Pageable pageable) {
        Page<ExpertProfile> profiles = (status == null)
                ? expertProfileRepository.findAll(pageable)
                : expertProfileRepository.findByStatus(status, pageable);
        return profiles.map(profile -> ExpertProfileResponse.from(profile));
    }

    @Transactional
    public ExpertProfileResponse revoke(Long expertProfileId, String reason) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.revoke(reason);
        profile.getUser().setRole(Role.USER);
        feedbackService.closeThreadsByExpertProfile(profile);
        return ExpertProfileResponse.from(profile);
    }

    public Page<PublicExpertResponse> getPublicList(Pageable pageable) {
        return expertProfileRepository.findByStatus(ExpertStatus.APPROVED, pageable)
                .map(profile -> PublicExpertResponse.from(profile));
    }

    public ExpertProfileDetailResponse getDetail(Long expertProfileId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .filter(ExpertProfile::isApproved)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
        return ExpertProfileDetailResponse.from(profile);
    }

    public ExpertApplicationStatusResponse getMyStatus(Long userId) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
        return ExpertApplicationStatusResponse.from(profile);
    }

    @Transactional
    public ExpertSignupResponse updateApplication(Long userId, ExpertSignupRequest request) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
        profile.updateApplication(request.getIntroduction());
        applyCareersAndCertifications(profile, request);
        return ExpertSignupResponse.from(profile);
    }

    private ExpertProfile getProfileOrThrow(Long id) {
        return expertProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND));
    }

    private void applyCareersAndCertifications(ExpertProfile profile, ExpertSignupRequest request) {
        profile.clearCareersAndCertifications();
        request.getCareers().forEach(c -> profile.addCareer(
                Career.builder()
                        .expertProfile(profile)
                        .companyName(c.getCompanyName())
                        .position(c.getPosition())
                        .years(c.getYears())
                        .jobField(c.getJobField())
                        .build()
        ));
        if (request.getCertifications() != null) {
            request.getCertifications().forEach(c -> profile.addCertification(
                    Certification.builder()
                            .expertProfile(profile)
                            .name(c.getName())
                            .issuer(c.getIssuer())
                            .acquiredYear(c.getAcquiredYear())
                            .build()
            ));
        }
    }
}