package org.example.backend.payment.client;

import org.example.backend.payment.dto.response.PortOneBillingKeyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// PortOne 빌링키 조회 클라이언트
@Component
public class PortOneBillingKeyClient {

    private final RestClient restClient = RestClient.create();

    @Value("${portone.api-secret}")
    private String apiSecret;

    @Value("${portone.store-id}")
    private String storeId;


    //빌링키 조회 API 기본주소
    private static final String PORTONE_BILLING_KEY_URL = "https://api.portone.io/billing-keys/";

    // 빌링키 단건 조회
    public PortOneBillingKeyResponse getBillingKey(String billingKey) {
        return restClient.get()
                .uri(PORTONE_BILLING_KEY_URL + billingKey + "?storeId=" + storeId)
                .header("Authorization", "PortOne " + apiSecret)
                .retrieve()
                .body(PortOneBillingKeyResponse.class);
    }
}