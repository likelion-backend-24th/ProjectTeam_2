package org.example.backend.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.service.EmailService;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.BillingKeyStatus;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.service.PaymentService;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;

    // "지금 살아있는 구독" - ACTIVE(정상 이용 중) 또는 PAST_DUE(유예기간 중, 재시도 진행 중).
    // 이 둘을 한 유저가 동시에 갖는 일은 없다는 전제(completePayment의 SUBSCRIPTION_ALREADY_ACTIVE
    // 가드 + PAST_DUE 정리 로직 참고).
    private static final List<SubscriptionStatus> LIVE_STATUSES =
            List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    @Transactional
    public SubscriptionResponse cancel(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatusIn(userId, LIVE_STATUSES)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        // (설계) 구독을 즉시 CANCELLED로 바꾸지 않고 빌링키만 삭제함 -> "해지 예약" 방식.
        // 환불 정책이 없어서 이미 낸 돈만큼(expiredAt까지)은 계속 이용하게 해주는 게 맞다고 판단.
        // 이번 결제 기간이 끝나면 renewSubscription이 "활성 빌링키 없음"으로 알아서 CANCELLED 처리함
        // -> 별도 Subscription 상태/필드 없이 기존 갱신 실패 로직을 그대로 재사용.
        // PAST_DUE 중인 구독도 동일하게 빌링키만 지우면 됨: 다음 스케줄러 실행(최대 1시간 내) 때
        // attemptRenewalCharge가 "활성 빌링키 없음"을 보고 finalizeCancellation으로 확정 취소해준다
        // (알림 메일도 "자진 해지"로 정확히 분기됨 - PaymentService.attemptRenewalCharge 참고).
        //
        // "해지"는 두 번(중복 클릭/여러 탭, 또는 정말 동시에 도착하는 두 요청) 호출해도 결과가 같아야
        // 하는(멱등) 액션이다. 구독 자체는 (ACTIVE든 PAST_DUE든) 상태가 바로 안 바뀌므로 두 번째
        // 요청도 여전히 같은 구독을 찾아 여기까지 들어온다.
        //
        // 활성 빌링키 행에 락을 걸고 조회해서 이 판단을 여기서 직접 내린다: 동시에 도착한 두 번째
        // 요청은 첫 번째 요청이 커밋될 때까지 이 조회에서 블록됐다가, 락이 풀리면 빌링키가 이미
        // 없어진 걸 보고 자연스럽게 "이미 해지됨"으로 처리된다.
        // (예전엔 PaymentService.deleteBillingKey(User,..)가 빌링키 없음을 예외로 던지고 여기서
        // catch해서 판단했는데, 그 호출이 다른 빈(bean)의 @Transactional 경계를 넘는 크로스빈 호출이라
        // Spring이 예외가 나가는 순간 공유 트랜잭션을 rollback-only로 표시해버려서, catch로 잡아 정상
        // 처리해도 커밋 시점에 UnexpectedRollbackException이 나는 문제가 있었음. 판단을 이 메서드 안에서
        // 끝내면 그 문제 자체가 생기지 않는다.)
        BillingKey billingKey = billingKeyRepository
                .findByUserAndStatusForUpdate(subscription.getUser(), BillingKeyStatus.ACTIVE)
                .orElse(null);

        if (billingKey != null) {
            paymentService.deleteBillingKey(billingKey, "사용자 요청에 의한 구독 해지");
            emailService.sendSubscriptionCancelled(subscription.getUser().getUsername());
        }
        // billingKey가 없으면 이미 해지된 상태 -> 조용히 넘어감 (이메일도 첫 요청 때 이미 보냈음)

        return SubscriptionResponse.from(subscription, false);
    }

    public SubscriptionResponse getMy(Long userId) {
        // PAST_DUE(유예기간 중)도 "지금 살아있는 구독"이라 여기 포함시킴 - 안 그러면 결제가 막 실패해서
        // 접근 권한이 끊긴 사용자가 "구독 없음"(404)으로만 보여서 본인 상태를 확인할 방법이 없었음.
        Subscription subscription = subscriptionRepository.findByUserIdAndStatusIn(userId, LIVE_STATUSES)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        boolean autoRenew = paymentService.hasActiveBillingKey(subscription.getUser());
        return SubscriptionResponse.from(subscription, autoRenew);
    }
}