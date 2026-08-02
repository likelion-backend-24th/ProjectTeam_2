package org.example.backend.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.user.dto.LoginRequest;
import org.example.backend.user.dto.SignupRequest;
import org.example.backend.user.dto.TokenResponse;
import org.example.backend.user.security.CustomUserDetails;
import org.example.backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    //회원가입 Post
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest signupRequest){
        userService.signup(signupRequest);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    //로그인 Post
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        TokenResponse response = userService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    //로그아웃 Post
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        userService.logout(customUserDetails.getUsername());
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}
