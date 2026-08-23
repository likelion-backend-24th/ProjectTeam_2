package org.example.backend.payment.client.dto;

/**
 * 빌링키 즉시 청구 본문. storeId가 없으면 이 계정에서는 조회/청구가 제대로 안 되므로 반드시 포함한다.
 */
public record PortOneBillingKeyPaymentRequest(
        String storeId,
        String billingKey,
        String orderName,
        Customer customer,
        Amount amount,
        String currency
) {
    public record Customer(String id) {
    }

    public record Amount(long total) {
    }
}
