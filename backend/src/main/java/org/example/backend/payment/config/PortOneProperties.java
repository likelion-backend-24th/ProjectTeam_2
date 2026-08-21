package org.example.backend.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "portone")
public class PortOneProperties {

    /**
     * PortOne Store ID (예: store-xxxx)
     */
    private String storeId;

    /**
     * 일반결제(인증결제) 채널 키
     */
    private String channelKeyPayment;

    /**
     * V2 API Secret. 절대 로그·프론트로 노출하지 않는다.
     */
    private String apiSecret;

    /**
     * PortOne REST API Base URL
     */
    private String apiBaseUrl = "https://api.portone.io";

    /**
     * paymentId 접두어. 팀/작성자 식별용 (예: p2g-ty-)
     */
    private String paymentIdPrefix;

    /**
     * 테스트 채널 여부. 학생 프로젝트는 항상 true — 이 값과 실제 결제의 테스트 여부가
     * 다르면 PortOne이 조회 자체를 거부한다 (실거래 오반영 방지).
     */
    private boolean testMode = true;

    @NestedConfigurationProperty
    private Subscription subscription = new Subscription();

    @Getter
    @Setter
    public static class Subscription {
        /**
         * 구독 고정 금액 (원 단위, 정수)
         */
        private long amount;

        /**
         * 통화
         */
        private String currency = "KRW";

        /**
         * 결제창에 표시할 주문명
         */
        private String orderName;
    }
}
