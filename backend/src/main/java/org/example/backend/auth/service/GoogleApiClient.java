package org.example.backend.auth.service;

import org.example.backend.auth.dto.oauth.GoogleUserInfoResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleApiClient {
    private final RestClient restClient = RestClient.create();

    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    public GoogleUserInfoResponse getUserInfo(String googleAccessToken) {
        return restClient.get()
                .uri(GOOGLE_USER_INFO_URL)
                .header("Authorization", "Bearer " + googleAccessToken)
                .retrieve()
                .body(GoogleUserInfoResponse.class);
    }
}
