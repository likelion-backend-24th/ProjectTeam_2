package org.example.backend.auth.controller;

import org.example.backend.auth.dto.jwt.TokenResponse;
import org.example.backend.common.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieResponseBuilder {
    // refreshtoken은 쿠키로 내려주기
    public ResponseEntity<ApiResponse<TokenResponse>> buildWithCookie(TokenResponse response, String message){
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 60 * 24 * 14)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(message,response));
    }

    public ResponseEntity<ApiResponse<Void>> buildWithCookieDeleted(String message) {
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.success(message, null));
    }


}
