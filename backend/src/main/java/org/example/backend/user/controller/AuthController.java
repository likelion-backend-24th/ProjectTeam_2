package org.example.backend.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.user.dto.LoginRequest;
import org.example.backend.user.dto.SignupRequest;
import org.example.backend.user.dto.TokenResponse;
import org.example.backend.user.security.CustomUserDetails;
import org.example.backend.user.service.UserService;
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

    private final UserService userService;

    //회원가입 Post
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest signupRequest){
        userService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }

    //로그인 Post
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        TokenResponse response = userService.login(loginRequest);
        return buildResponseWithCookie(response);
    }

    //재발급 Post
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @CookieValue(name = "refreshToken") String refreshToken){
        TokenResponse response = userService.reissue(refreshToken);
        return buildResponseWithCookie(response);
    }

    //로그아웃 Post
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        userService.logout(customUserDetails.getUsername());

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


    // refreshtoken은 쿠키로 내려주기
    private ResponseEntity<TokenResponse> buildResponseWithCookie(TokenResponse response){
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 60 * 24 * 14) // 14일 (초 단위)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }


}
