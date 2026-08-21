package org.example.backend.payment.dto;

import lombok.Getter;

// 빌링키 결제 요청 (포트원한테 요청하는 DTO)
@Getter
public class PortOneBillingKeyChargeRequest {

    private String storeId;
    private String billingKey;
    private String orderName;
    private Amount amount;
    private String currency;

    public PortOneBillingKeyChargeRequest(String storeId, String billingKey, String orderName, Integer totalAmount, String currency) {
        this.storeId = storeId;
        this.billingKey = billingKey;
        this.orderName = orderName;
        this.amount = new Amount(totalAmount);
        this.currency = currency;
    }

    // 결제 요청 금액 (PortOne이 요구하는 중첩 구조)
    @Getter
    public static class Amount {
        private Integer total;

        public Amount(Integer total) {
            this.total = total;
        }
    }
}