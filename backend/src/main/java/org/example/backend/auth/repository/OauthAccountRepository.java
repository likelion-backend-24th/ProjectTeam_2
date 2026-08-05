package org.example.backend.auth.repository;

import org.example.backend.auth.entity.OauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthAccountRepository extends JpaRepository<OauthAccount,Long> {
    // provider=Kakao나 Google,Naver providerID=카카오나 구글,네이버고유ID 조회
    Optional<OauthAccount> findByProviderAndProviderId(String provider, String providerId);
}
