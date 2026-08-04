package org.example.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.dto.oauth.KakaoUserInfoResponse;
import org.example.backend.auth.entity.OauthAccount;
import org.example.backend.auth.exception.*;
import org.example.backend.auth.dto.jwt.LoginRequest;
import org.example.backend.auth.dto.jwt.SignupRequest;
import org.example.backend.auth.dto.jwt.TokenResponse;
import org.example.backend.auth.entity.AccountStatus;
import org.example.backend.auth.entity.RefreshToken;
import org.example.backend.auth.repository.OauthAccountRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.auth.repository.RefreshTokenRepository;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.auth.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KakaoApiClient kakaoApiClient;
    private final OauthAccountRepository oauthAccountRepository;

    //회원가입
    @Transactional
    public void signup(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new DuplicateUsernameException("이미 가입된 이메일입니다.");
        }
        if (userRepository.existsByNickname(signupRequest.getNickname())) {
            throw new DuplicateNicknameException(("이미 사용 중인 닉네임입니다."));
        }
        User user = new User();
        user.setName(signupRequest.getName());
        user.setUsername(signupRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setNickname(signupRequest.getNickname());
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSubscribed(false);

        userRepository.save(user);
    }

    //로그인
    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 계정입니다."));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new InactiveAccountException("정지되었거나 탈퇴한 계정입니다.");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // 리프레쉬토큰 DB에 저장
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(14));

        refreshTokenRepository.save(refreshToken);


        return new TokenResponse(accessToken, refreshTokenValue);
    }

    //재발급
    @Transactional
    public TokenResponse reissue(String refreshTokenValue) {
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new InvalidRefreshTokenException("유효하지 않은 refresh token입니다.");
        }

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("존재하지 않는 refresh token입니다."));

        User user = savedRefreshToken.getUser();

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        savedRefreshToken.setToken(newRefreshToken);
        savedRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(14));
        refreshTokenRepository.save(savedRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }


    //로그아웃
    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        refreshTokenRepository.deleteByUser(user);
    }

    // Kakao 최초 로그인 시 회원가입
    public User registerKakaoUser(KakaoUserInfoResponse kakaoUserInfo, String providerId){
        User user = new User();
        user.setUsername("kakao_" + providerId + "@kakao.local");
        user.setName(kakaoUserInfo.getKakao_account().getProfile().getNickname());
        user.setNickname(kakaoUserInfo.getKakao_account().getProfile().getNickname());
        user.setPassword(null);  //카카오에서 실명을 주지 않아서 일단 닉네임으로 채우고
        user.setRole(Role.USER); // 나중에 마이페이지에서 닉네임 수정 유도
        user.setStatus(AccountStatus.ACTIVE);
        user.setSubscribed(false);

        userRepository.save(user);

        OauthAccount oauthAccount = new OauthAccount();
        oauthAccount.setUser(user);
        oauthAccount.setProvider("KAKAO");
        oauthAccount.setProviderId(providerId);
        oauthAccount.setLinkedAt(LocalDateTime.now());

        oauthAccountRepository.save(oauthAccount);

        return user;
    }

    // Kakao 로그인
    public TokenResponse kakaoLogin(String kakaoAccessToken) {
        KakaoUserInfoResponse kakaoUserInfo = kakaoApiClient.getUserInfo(kakaoAccessToken);

        String providerId = String.valueOf(kakaoUserInfo.getId());

        User user = oauthAccountRepository.findByProviderAndProviderId("KAKAO", providerId)
                .map(oauthAccount -> oauthAccount.getUser())
                .orElseGet(() -> registerKakaoUser(kakaoUserInfo, providerId));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // 리프레쉬토큰 DB에 저장
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(14));

        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue);

    }

}