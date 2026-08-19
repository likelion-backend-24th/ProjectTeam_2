package org.example.backend.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SubscriptionPlanType {
    BASIC(9900);

    private final int amount;
}
