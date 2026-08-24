package org.example.backend.payment.service;

/**
 * 빌링키 청구가 어떤 맥락에서 일어났는지. 청구 로직 자체는 같고, 성공/실패 후처리만 달라진다.
 */
public enum SubscriptionChargeMode {

    /** 카드 등록 직후의 최초 구독. 성공하면 새 구독을 만든다. */
    INITIAL(false),

    /** 스케줄러의 자동 갱신. 실패를 누적해 3회 연속 실패하면 구독을 만료시킨다. */
    SCHEDULED_RENEWAL(true),

    /**
     * 사용자가 화면에서 직접 누른 재시도. 실패해도 자동 재시도 횟수를 깎지 않는다 —
     * 잔액이 없는 상태에서 버튼을 세 번 누르면 남은 유예 기간이 그 자리에서 사라지기 때문이다.
     */
    MANUAL_RETRY(false);

    private final boolean countsFailure;

    SubscriptionChargeMode(boolean countsFailure) {
        this.countsFailure = countsFailure;
    }

    /** 실패를 구독의 재시도 횟수로 누적할지. */
    public boolean countsFailure() {
        return countsFailure;
    }

    /** 기존 구독의 기간을 연장하는 청구인지(= 최초 구독이 아닌지). */
    public boolean isRenewal() {
        return this != INITIAL;
    }
}
