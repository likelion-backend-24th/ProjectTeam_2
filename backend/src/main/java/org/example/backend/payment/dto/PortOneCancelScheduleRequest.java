package org.example.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 결제 예약 취소 요청
@Getter
@AllArgsConstructor
public class PortOneCancelScheduleRequest {

    private String storeId;
    private String billingKey;

}