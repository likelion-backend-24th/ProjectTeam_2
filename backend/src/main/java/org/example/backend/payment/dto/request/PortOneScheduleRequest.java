package org.example.backend.payment.dto.request;

import lombok.Getter;

import java.util.List;

// 결제 예약 요청
@Getter
public class PortOneScheduleRequest {

    private PaymentInput payment;
    private String timeToPay;

    public PortOneScheduleRequest(String storeId, String billingKey, String orderName, Integer totalAmount, String currency, String timeToPay, String noticeUrl) {
        this.payment = new PaymentInput(storeId, billingKey, orderName, totalAmount, currency, noticeUrl);
        this.timeToPay = timeToPay;
    }

    // PortOne이 요구하는 결제 입력 정보 (중첩 구조)
    @Getter
    public static class PaymentInput {
        private String storeId;
        private String billingKey;
        private String orderName;
        private Amount amount;
        private String currency;
        private List<String> noticeUrls;

        public PaymentInput(String storeId, String billingKey, String orderName, Integer totalAmount, String currency, String noticeUrl) {
            this.storeId = storeId;
            this.billingKey = billingKey;
            this.orderName = orderName;
            this.amount = new Amount(totalAmount);
            this.currency = currency;
            this.noticeUrls = noticeUrl == null || noticeUrl.isBlank() ? null : List.of(noticeUrl);
        }
    }

    // 결제 금액 (PortOne이 요구하는 중첩 구조)
    @Getter
    public static class Amount {
        private Integer total;

        public Amount(Integer total) {
            this.total = total;
        }
    }
}