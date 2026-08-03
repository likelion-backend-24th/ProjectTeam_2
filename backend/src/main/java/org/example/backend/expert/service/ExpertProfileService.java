package org.example.backend.expert.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.expert.dto.ExpertProfileResponse;
import org.example.backend.expert.dto.ExpertSignupRequest;
import org.example.backend.expert.dto.ExpertListResponse;
import org.example.backend.expert.dto.ExpertSignupResponse;
import org.example.backend.expert.dto.PublicExpertResponse;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;
import org.example.backend.expert.repository.ExpertProfileRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository; // TODO: 실제 패키지 확인 필요
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertProfileService {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;

    // F-25: 전문가 신청 (로그인한 USER). 거절(REJECTED) 이력이 있으면 재신청 가능
    @Transactional
    public ExpertSignupResponse signup(Long userId, ExpertSignupRequest request) {
        return expertProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.reapply(request.getCareer(), request.getCertification()); // PENDING/APPROVED면 내부에서 예외
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

    // F-26: ADMIN 승인 -> role=EXPERT 전환
    @Transactional
    public ExpertProfileResponse approve(Long expertProfileId) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.approve();
        profile.getUser().setRole(Role.EXPERT);
        return ExpertProfileResponse.from(profile);
    }

    // F-26: ADMIN 거절
    @Transactional
    public ExpertProfileResponse reject(Long expertProfileId, String reason) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.reject(reason);
        return ExpertProfileResponse.from(profile);
    }

    // F-27: ADMIN 목록 조회 (status=null이면 전체)
    public ExpertListResponse getList(ExpertStatus status) {
        List<ExpertProfile> profiles = (status == null)
                ? expertProfileRepository.findAll()
                : expertProfileRepository.findByStatus(status);
        List<ExpertProfileResponse> responses = profiles.stream().map(ExpertProfileResponse::from).toList();
        return ExpertListResponse.from(responses);
    }

    // F-27: ADMIN 자격 박탈 -> role=USER로 원복
    @Transactional
    public ExpertProfileResponse revoke(Long expertProfileId, String reason) {
        ExpertProfile profile = getProfileOrThrow(expertProfileId);
        profile.revoke(reason);
        profile.getUser().setRole(Role.USER);
        return ExpertProfileResponse.from(profile);
    }

    // F-32: 승인된 전문가 목록 공개 조회 (비로그인 포함 전체 공개, 인증 불필요)
    public List<PublicExpertResponse> getPublicList() {
        return expertProfileRepository.findByStatus(ExpertStatus.APPROVED)
                .stream().map(PublicExpertResponse::from).toList();
    }

    private ExpertProfile getProfileOrThrow(Long id) {
        return expertProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전문가 프로필입니다."));
    }
}