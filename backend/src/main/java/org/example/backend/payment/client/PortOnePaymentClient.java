package org.example.backend.payment.client;

import org.example.backend.payment.dto.PortOneBillingKeyChargeRequest;
import org.example.backend.payment.dto.PortOnePaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// PortOne 결제 관련 API 호출 클라이언트
@Component
public class PortOnePaymentClient {

    private final RestClient restClient = RestClient.create();

    @Value("${portone.api-secret}")
    private String apiSecret;

    @Value("${portone.store-id}")
    private String storeId;

    private static final String PORTONE_PAYMENT_URL = "https://api.portone.io/payments/";

    // 결제 단건 조회 (검증용 재조회)
    public PortOnePaymentResponse getPayment(String paymentId) {
        return restClient.get()
                .uri(PORTONE_PAYMENT_URL + paymentId + "?storeId=" + storeId)
                .header("Authorization", "PortOne " + apiSecret)
                .retrieve()
                .body(PortOnePaymentResponse.class);
    }

    // 빌링키로 결제 실행
    public PortOnePaymentResponse payWithBillingKey(String paymentId, String billingKey, String orderName, Integer amount) {
        PortOneBillingKeyChargeRequest request =
                new PortOneBillingKeyChargeRequest(storeId, billingKey, orderName, amount, "KRW");

        return restClient.post()
                .uri(PORTONE_PAYMENT_URL + paymentId + "/billing-key")
                .header("Authorization", "PortOne " + apiSecret)
                .body(request)
                .retrieve()
                .body(PortOnePaymentResponse.class);
    }
}