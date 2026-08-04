package org.example.backend.user;

import org.example.backend.auth.dto.oauth.KakaoUserInfoResponse;
import org.example.backend.auth.service.KakaoApiClient;

public class KakaoApiClientTest {
    public static void main(String[] args) {
        KakaoApiClient kakaoApiClient = new KakaoApiClient();              //토큰 받는 법은 OAuth.http 참고
        KakaoUserInfoResponse response = kakaoApiClient.getUserInfo("DUduzZz0.....카카오엑세스 토큰");
        System.out.println("카카오 고유 ID : " + response.getId());
        System.out.println("닉네임 : " + response.getKakao_account().getProfile().getNickname());

    }
}
