package org.example.backend.auth.service;

import org.example.backend.auth.dto.oauth.NaverUserInfoResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NaverApiClient {

    private final RestClient restClient = RestClient.create();

    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    public NaverUserInfoResponse getUserInfo(String naverAccessToken) {
        return restClient.get()
                .uri(NAVER_USER_INFO_URL)
                .header("Authorization", "Bearer " + naverAccessToken)
                .retrieve()
                .body(NaverUserInfoResponse.class);
    }

}
