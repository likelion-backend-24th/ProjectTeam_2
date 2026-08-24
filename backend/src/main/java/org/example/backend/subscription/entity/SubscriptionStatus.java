package org.example.backend.subscription.entity;

import java.util.List;

public enum SubscriptionStatus {
    /** 정상 이용 중 (자동 갱신 여부와 무관하게 만료 전이면 이 상태) */
    ACTIVE,
    /** 정기결제 실패로 재시도 중. 만료 전까지는 계속 이용 가능 */
    PAST_DUE,
    /** 만료되어 더 이상 이용 불가 */
    EXPIRED;

    /** 아직 이용 가능한 상태. 조회·갱신·만료 대상 판단에 공통으로 쓴다. */
    public static final List<SubscriptionStatus> USABLE = List.of(ACTIVE, PAST_DUE);
}
