package org.example.backend.auth.service;

import org.example.backend.auth.dto.oauth.KakaoUserInfoResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoApiClient {

                //외부서버에 HTTP 요청 보내는 도구
                //왜냐 우리는 브라우저 대신 RESTAPI 방식이니까
    private final RestClient restClient = RestClient.create();

    //카카오가 공식적으로 제공하는 내 정보 조회 API주소
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    // 엑세스토큰을 받아서 카카오한테 물어보고 정보돌려주는 메서드
    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        return restClient.get() //GET방식으로 보냄
                .uri(KAKAO_USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserInfoResponse.class);
    }
}

