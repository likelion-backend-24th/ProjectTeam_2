package org.example.backend.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

// PortOne 결제 단건 조회/빌링키 결제 응답 (검증에 필요한 필드만 매핑)
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOnePaymentResponse {

    private String status;
    private String id;
    private String storeId;
    private Channel channel;
    private Amount amount;
    private String currency;

    // 결제에 사용된 채널 정보
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {
        private String type; // 지금은 test
        private String key;
    }

    // 결제 금액 정보
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private Integer total;
    }
}