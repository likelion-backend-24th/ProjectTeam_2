package org.example.backend.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.dto.jwt.LoginRequest;
import org.example.backend.auth.dto.jwt.SignupRequest;
import org.example.backend.auth.dto.jwt.TokenResponse;
import org.example.backend.auth.dto.oauth.GoogleLoginRequest;
import org.example.backend.auth.dto.oauth.KakaoLoginRequest;
import org.example.backend.auth.dto.oauth.NaverLoginRequest;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.auth.service.AuthService;
import org.example.backend.common.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //회원가입 Post
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest signupRequest){
        authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }

    //로그인 Post
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        TokenResponse response = authService.login(loginRequest);
        return buildResponseWithCookie(response);
    }

    //재발급 Post
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @CookieValue(name = "refreshToken") String refreshToken){
        TokenResponse response = authService.reissue(refreshToken);
        return buildResponseWithCookie(response);
    }

    //로그아웃 Post
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        authService.logout(customUserDetails.getUsername());

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("로그아웃되었습니다.");
    }

    // Kakao 로그인 Post
    @PostMapping("/kakao")
    public ResponseEntity<TokenResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest kakaoLoginRequest){
        TokenResponse response = authService.kakaoLogin(kakaoLoginRequest.getKakaoAccessToken());
        return buildResponseWithCookie(response);
    }

    // Google 로그인 Post
    @PostMapping("/google")
    public ResponseEntity<TokenResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest googleLoginRequest){
        TokenResponse response = authService.googleLogin(googleLoginRequest.getGoogleAccessToken());
        return buildResponseWithCookie(response);
    }

    // NAVER 로그인 Post
    @PostMapping("/naver")
    public ResponseEntity<TokenResponse> naverLogin(@Valid @RequestBody NaverLoginRequest naverLoginRequest){
        TokenResponse response = authService.naverLogin(naverLoginRequest.getNaverAccessToken());
        return buildResponseWithCookie(response);
    }


}
