package org.example.backend.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SubscriptionPlanType {
    BASIC(9900, "prep2gether 베이직 구독");

    private final int amount;
    private final String orderName;
}
