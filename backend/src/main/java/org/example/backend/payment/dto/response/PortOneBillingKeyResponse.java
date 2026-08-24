package org.example.backend.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// PortOne 빌링키 단건 조회 응답
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) //응답에러 방지
public class PortOneBillingKeyResponse {

    private String status;  // 발급완료인지 삭제된건지
    private String billingKey;
    private List<Channel> channels; //하나의 빌링키가 여러 pg사 채널에 걸쳐 등록 될수도 있는 구조라서

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {
        private String type;
        private String key;
    }
}