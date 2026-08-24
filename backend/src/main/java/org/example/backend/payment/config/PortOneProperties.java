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
     * 정기결제(빌링키) 채널 키
     */
    private String channelKeyBilling;

    /**
     * V2 API Secret. 절대 로그·프론트로 노출하지 않는다.
     */
    private String apiSecret;

    /**
     * 빌링키 암호화 저장용 시크릿.
     */
    private String billingKeySecret;

    /**
     * PortOne REST API Base URL
     */
    private String apiBaseUrl = "https://api.portone.io";

    /**
     * 웹훅 서명 검증용 시크릿. PortOne 콘솔 > 웹훅 설정에서 발급.
     */
    private String webhookSecret;

    /**
     * paymentId 접두어. 팀/작성자 식별용 (예: p2g-ty-)
     */
    private String paymentIdPrefix;

    /**
     * 테스트 채널 여부. 학생 프로젝트는 항상 true.
     * 결제 검증 시 PortOne 응답의 channel.type(TEST/LIVE)과 대조해 환경 오반영을 막는다.
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
