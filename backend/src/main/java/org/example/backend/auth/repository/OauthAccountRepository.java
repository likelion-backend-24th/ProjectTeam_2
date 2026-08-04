package org.example.backend.auth.repository;

import org.example.backend.auth.entity.OauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthAccountRepository extends JpaRepository<OauthAccount,Long> {
    // provider=kakao나 구글 providerID=카카오나 구글고유ID 조회
    Optional<OauthAccount> findByProviderAndProviderId(String provider, String providerId);
}
