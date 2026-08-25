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
import org.example.backend.user.exception.UserErrorCode;
import org.example.backend.user.repository.UserRepository;
import org.example.backend.auth.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

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
    private final LoginAttemptService loginAttemptService;

    //회원가입
    @Transactional
    public void signup(SignupRequest signupRequest) {
        Optional<User> existingUser = userRepository.findByUsername(signupRequest.getUsername());
        // 유저가 존재하고 비밀번호도 갖고있으면 중복으로 회원가입 불가
        if(existingUser.isPresent() && existingUser.get().getPassword() != null){
            throw new BusinessException(AuthErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByNickname(signupRequest.getNickname())) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_NICKNAME);
        }
        if (!signupRequest.isTermsAgreed()) {
            throw new BusinessException(AuthErrorCode.TERMS_NOT_AGREED);
        }
        // 이메일 인증이 되어있는지
        emailVerificationService.checkVerified(signupRequest.getUsername());
        // 기존 소셜 계정에 비밀번호만 연결
        if(existingUser.isPresent()){
            User user = existingUser.get();
            user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            userRepository.save(user);
            return; //여기서 메서드 종료해야함 밑으로 가면 안됨.
        }

        User user = new User();
        user.setName(signupRequest.getName());
        user.setUsername(signupRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setNickname(signupRequest.getNickname());
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSubscribed(false);
        user.setTermsAgreeAt(LocalDateTime.now());

        userRepository.save(user);
    }

    //로그인
    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        // 잠금 상태 확인
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())){
            throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailedLoginAttempt(user);
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
        }
        //로그인 성공하면 다시 초기화
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        // 회원 탈퇴/정지 계정인지 체크
        checkAccountActive(user);

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

        // 회원 탈퇴/정지 계정인지 체크
        checkAccountActive(user);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        savedRefreshToken.setToken(newRefreshToken);
        savedRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(14));
        refreshTokenRepository.save(savedRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    //비밀번호 재설정(찾기) (이메일 인증 완료 후, 로그인 없이 새 빌밀번호 설정)
    @Transactional
    public void resetPassword(String username,String newPassword){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        if(user.getPassword() == null){
            throw new BusinessException(UserErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
        }

        emailVerificationService.checkVerified(username);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);
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
        user.setNickname(generateUniqueNickname(kakaoUserInfo.getKakao_account().getProfile().getNickname())); //뒤에 랜덤 숫자4자리 붙임
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

        // 회원 탈퇴/정지 계정인지 체크
        checkAccountActive(user);

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

    // Google 최초 로그인 시 회원가입 또는 기존 계정 연결
    private User registerGoogleUser(GoogleUserInfoResponse googleUserInfo,String providerId){
        User user = userRepository.findByUsername(googleUserInfo.getEmail())
                .orElseGet(() -> createNewGoogleUser(googleUserInfo));

        OauthAccount oauthAccount = new OauthAccount();
        oauthAccount.setUser(user);
        oauthAccount.setProvider("GOOGLE");
        oauthAccount.setProviderId(providerId);
        oauthAccount.setLinkedAt(LocalDateTime.now());

        oauthAccountRepository.save(oauthAccount);

        return user;
    }

    // 완전히 새로운 구글 유저 생성
    private User createNewGoogleUser(GoogleUserInfoResponse googleUserInfo) {
        User user = new User();
        user.setUsername(googleUserInfo.getEmail());
        user.setName(googleUserInfo.getName());
        user.setNickname(generateUniqueNickname(googleUserInfo.getName())); // 뒤에 랜덤 숫자 4자리 붙임
        user.setPassword(null);
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSubscribed(false);

        return userRepository.save(user);
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

        // 회원 탈퇴/정지 계정인지 체크
        checkAccountActive(user);

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
        User user = userRepository.findByUsername(naverUserInfo.getResponse().getEmail())
                .orElseGet(() -> createNewNaverUser(naverUserInfo));

        OauthAccount oauthAccount = new OauthAccount();
        oauthAccount.setUser(user);
        oauthAccount.setProvider("NAVER");
        oauthAccount.setProviderId(prviderId);
        oauthAccount.setLinkedAt(LocalDateTime.now());

        oauthAccountRepository.save(oauthAccount);

        return user;
    }

    private User createNewNaverUser(NaverUserInfoResponse naverUserInfo) {
        User user = new User();
        user.setUsername(naverUserInfo.getResponse().getEmail());
        user.setName(naverUserInfo.getResponse().getName());
        user.setNickname(generateUniqueNickname(naverUserInfo.getResponse().getName())); // 뒤에 숫자 4자리 붙임
        user.setPassword(null);
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSubscribed(false);

        return userRepository.save(user);
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

        // 회원 탈퇴/정지 계정인지 체크
        checkAccountActive(user);

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


    // 이름 뒤에 랜덤4자리 숫자 붙여서 닉네임 생성 메서드
    private String generateUniqueNickname(String name){
        Random random = new Random();
        String nickname;

        do{
            int randomNumber = random.nextInt(10000);
            nickname = name + "_" + randomNumber;
        }while (userRepository.existsByNickname(nickname));

        return nickname;
    }

    // 계정 상태 확인 (정지/탈퇴 여부에 따라 다른 메시지로 응답)
    private void checkAccountActive(User user) {
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == AccountStatus.WITHDRAWN) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_WITHDRAWN);
        }
    }

}