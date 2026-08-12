package org.example.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.dto.oauth.GoogleUserInfoResponse;
import org.example.backend.auth.dto.oauth.KakaoUserInfoResponse;
import org.example.backend.auth.dto.oauth.NaverUserInfoResponse;
import org.example.backend.auth.entity.OauthAccount;
import org.example.backend.auth.exception.*;
import org.example.backend.auth.dto.jwt.LoginRequest;
import org.example.backend.auth.dto.jwt.SignupRequest;
import org.example.backend.auth.dto.jwt.TokenResponse;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.auth.entity.RefreshToken;
import org.example.backend.auth.repository.OauthAccountRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.auth.repository.RefreshTokenRepository;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.auth.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KakaoApiClient kakaoApiClient;
    private final OauthAccountRepository oauthAccountRepository;
    private final GoogleApiClient googleApiClient;
    private final NaverApiClient naverApiClient;
    private final EmailVerificationService emailVerificationService;

    //회원가입
    @Transactional
    public void signup(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByNickname(signupRequest.getNickname())) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_NICKNAME);
        }
        // 이메일 인증이 되어있는지
        emailVerificationService.checkVerified(signupRequest.getUsername());

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
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.INACTIVE_ACCOUNT);
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
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

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
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUser(user);
    }

    // Kakao 최초 로그인 시 회원가입
    private User registerKakaoUser(KakaoUserInfoResponse kakaoUserInfo, String providerId){
        User user = new User();
        user.setUsername("kakao_" + providerId + "@kakao.local");
        user.setName(kakaoUserInfo.getKakao_account().getProfile().getNickname());
        user.setNickname(kakaoUserInfo.getKakao_account().getProfile().getNickname() + "_KAKAO");
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
    @Transactional
    public TokenResponse kakaoLogin(String kakaoAccessToken) {
        KakaoUserInfoResponse kakaoUserInfo;
        try {
            kakaoUserInfo = kakaoApiClient.getUserInfo(kakaoAccessToken);
        } catch (HttpClientErrorException e) {
            throw new BusinessException(AuthErrorCode.OAUTH_TOKEN_INVALID);
        }

        String providerId = String.valueOf(kakaoUserInfo.getId());

        User user = oauthAccountRepository.findByProviderAndProviderId("KAKAO", providerId)
                .map(oauthAccount -> oauthAccount.getUser())
                .orElseGet(() -> registerKakaoUser(kakaoUserInfo, providerId));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.INACTIVE_ACCOUNT);
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

    // Google 최초 로그인 시 회원가입 처리
    private User registerGoogleUser(GoogleUserInfoResponse googleUserInfo,String providerId){
        User user = new User();
        user.setUsername(googleUserInfo.getEmail()); //카카오는 이메일권한 막혀있지만 구글은 허용되어서 좋음
        user.setName(googleUserInfo.getName()); // 이름도 권한 있어서 사용가능
        user.setNickname(googleUserInfo.getName() + "_GOOGLE"); //이건 나중에 마이페이지에서 닉네임 변경
        user.setPassword(null);
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSubscribed(false);

        userRepository.save(user);

        OauthAccount oauthAccount = new OauthAccount();
        oauthAccount.setUser(user);
        oauthAccount.setProvider("GOOGLE");
        oauthAccount.setProviderId(providerId);
        oauthAccount.setLinkedAt(LocalDateTime.now());

        oauthAccountRepository.save(oauthAccount);

        return user;
    }

    // Google 로그인
    @Transactional
    public TokenResponse googleLogin(String googleAccessToken){
        GoogleUserInfoResponse googleUserInfo;
        try {
            googleUserInfo = googleApiClient.getUserInfo(googleAccessToken);
        } catch (HttpClientErrorException e) {
            throw new BusinessException(AuthErrorCode.OAUTH_TOKEN_INVALID);
        }

        String providerId = googleUserInfo.getId();

        User user = oauthAccountRepository.findByProviderAndProviderId("GOOGLE", providerId)
                .map(oauthAccount -> oauthAccount.getUser())
                .orElseGet(() -> registerGoogleUser(googleUserInfo, providerId));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.INACTIVE_ACCOUNT);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getUsername());

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(14));

        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue);
    }

    // NAVER 최초 로그인 시 회원가입 처리
    private User registerNaverUser(NaverUserInfoResponse naverUserInfo,String prviderId){
        User user = new User();
        user.setUsername(naverUserInfo.getResponse().getEmail());
        user.setName(naverUserInfo.getResponse().getName());
        user.setNickname(naverUserInfo.getResponse().getName() + "_NAVER");
        user.setPassword(null);
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSubscribed(false);

        userRepository.save(user);

        OauthAccount oauthAccount = new OauthAccount();
        oauthAccount.setUser(user);
        oauthAccount.setProvider("NAVER");
        oauthAccount.setProviderId(prviderId);
        oauthAccount.setLinkedAt(LocalDateTime.now());

        oauthAccountRepository.save(oauthAccount);

        return user;

    }

    // NAVER 로그인
    @Transactional
    public TokenResponse naverLogin(String naverAccessToken){
        NaverUserInfoResponse naverUserInfo;
        try {
            naverUserInfo = naverApiClient.getUserInfo(naverAccessToken);
        } catch (HttpClientErrorException e) {
            throw new BusinessException(AuthErrorCode.OAUTH_TOKEN_INVALID);
        }

        String providerId = naverUserInfo.getResponse().getId();

        User user = oauthAccountRepository.findByProviderAndProviderId("NAVER", providerId)
                .map(oauthAccount -> oauthAccount.getUser())
                .orElseGet(() -> registerNaverUser(naverUserInfo, providerId));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.INACTIVE_ACCOUNT);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getUsername());

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(14));

        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue);
    }

}