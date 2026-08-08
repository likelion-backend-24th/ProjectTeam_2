package org.example.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증", description = "회원가입, 로그인, 토큰 재발급, 로그아웃, 소셜 로그인 API")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieResponseBuilder authCookieResponseBuilder;

    //회원가입 Post
    @Operation(summary = "회원가입", description = "이름, 이메일(username), 비밀번호, 닉네임으로 회원가입 합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest signupRequest){
        authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("회원가입이 완료되었습니다.", null));
    }

    //로그인 Post
    @Operation(summary = "로그인",description = "이메일(username)과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest){
        TokenResponse response = authService.login(loginRequest);
        return authCookieResponseBuilder.buildWithCookie(response,"로그인 성공");
    }

    //재발급 Post
    @Operation(summary = "토큰 재발급", description = "쿠키에 담긴 refreshToken으로 accessToken을 재발급받습니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @CookieValue(name = "refreshToken") String refreshToken){
        TokenResponse response = authService.reissue(refreshToken);
        return authCookieResponseBuilder.buildWithCookie(response,"토큰 재발급 성공");
    }

    //로그아웃 Post
    @Operation(summary = "로그아웃", description = "refreshToken을 서버와 쿠키에서 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        authService.logout(customUserDetails.getUsername());
        return authCookieResponseBuilder.buildWithCookieDeleted("로그아웃되었습니다.");
    }

    // Kakao 로그인 Post
    @Operation(summary = "카카오 로그인",description = "카카오 엑세스 토큰으로 로그인, 최초 로그인 시 자동 회원가입됩니다.")
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(@Valid @RequestBody KakaoLoginRequest kakaoLoginRequest){
        TokenResponse response = authService.kakaoLogin(kakaoLoginRequest.getKakaoAccessToken());
        return authCookieResponseBuilder.buildWithCookie(response,"카카오 로그인 성공");
    }

    // Google 로그인 Post
    @Operation(summary = "구글 로그인",description = "카카오 엑세스 토큰으로 로그인, 최초 로그인 시 자동 회원가입됩니다.")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<TokenResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest googleLoginRequest){
        TokenResponse response = authService.googleLogin(googleLoginRequest.getGoogleAccessToken());
        return authCookieResponseBuilder.buildWithCookie(response,"구글 로그인 성공");
    }

    // NAVER 로그인 Post
    @Operation(summary = "네이버 로그인",description = "카카오 엑세스 토큰으로 로그인, 최초 로그인 시 자동 회원가입됩니다.")
    @PostMapping("/naver")
    public ResponseEntity<ApiResponse<TokenResponse>> naverLogin(@Valid @RequestBody NaverLoginRequest naverLoginRequest){
        TokenResponse response = authService.naverLogin(naverLoginRequest.getNaverAccessToken());
        return authCookieResponseBuilder.buildWithCookie(response,"네이버 로그인 성공");
    }
}
