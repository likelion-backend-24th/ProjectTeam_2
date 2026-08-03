package org.example.backend.expert.controller.support;

import org.springframework.security.core.Authentication;

public final class AuthUtil {

    private AuthUtil() {
    }

    public static Long resolveUserId(Authentication authentication) {
        throw new UnsupportedOperationException(
                "AuthUtil.resolveUserId 구현이 필요합니다. 팀 Security 설정(로그인 principal 타입) 확인 후 채워주세요.");
    }
}