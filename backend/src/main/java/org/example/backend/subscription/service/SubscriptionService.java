package org.example.backend.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.service.EmailService;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.dto.PaymentPrepareResponse;
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
        // PAST_DUE 중인 구독도 동일하게 빌링키만 지우면 됨: 다음 스케줄러 실행 때
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

    // UserService.withdrawAccount()가 "정리할 구독이 있는지"를 먼저 확인할 때 씀.
    // cancel()을 그냥 불러보고 SUBSCRIPTION_NOT_FOUND를 catch하는 방식은 쓰지 않는다: cancel()이
    // 크로스빈 @Transactional 호출이라, 예외가 나가는 순간 Spring이 호출자의 공유 트랜잭션을
    // rollback-only로 표시해버려서(catch 지점보다 먼저 개입함) catch로 잡아 정상 진행해도 커밋
    // 시점에 UnexpectedRollbackException이 나는 문제가 있다 - deleteBillingKey에서 겪었던 것과
    // 동일한 함정. 그래서 "실패할 수 있는 크로스빈 호출"을 아예 피하고 단순 조회로 먼저 판단한다.
    public boolean hasLiveSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndStatusIn(userId, LIVE_STATUSES).isPresent();
    }

    public SubscriptionResponse getMy(Long userId) {
        // PAST_DUE(유예기간 중)도 "지금 살아있는 구독"이라 여기 포함시킴 - 안 그러면 결제가 막 실패해서
        // 접근 권한이 끊긴 사용자가 "구독 없음"(404)으로만 보여서 본인 상태를 확인할 방법이 없었음.
        Subscription subscription = subscriptionRepository.findByUserIdAndStatusIn(userId, LIVE_STATUSES)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        boolean autoRenew = paymentService.hasActiveBillingKey(subscription.getUser());
        return SubscriptionResponse.from(subscription, autoRenew);
    }

    // 해지 예약 취소(자동갱신 재개)를 위한 새 빌링키 발급 파라미터 준비.
    // PAST_DUE(유예기간 중)는 여기 대상이 아님 - 그쪽은 접근 권한이 이미 끊긴 상태라 완전히 새로
    // 결제하는 completePayment 경로로 복구하는 게 맞고(카드 실패 이력이 있어 재시도 가치 판단이 다름),
    // 여기는 "아직 멀쩡히 ACTIVE인데 해지만 예약된" 경우만 다룬다.
    public PaymentPrepareResponse prepareResume(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 이미 자동갱신 중이면(해지 예약 자체를 안 했거나 이미 재개됨) 굳이 PortOne 발급창을 띄울
        // 이유가 없으니 여기서 먼저 막는다. (실제 저장 시점의 중복 방지는 attachBillingKey가 별도로 함)
        if (paymentService.hasActiveBillingKey(subscription.getUser())) {
            throw new BusinessException(SubscriptionErrorCode.AUTO_RENEW_ALREADY_ON);
        }

        return paymentService.prepareBillingKeyReissue(subscription.getUser(), subscription.getPlanType());
    }

    // 유예기간(PAST_DUE) 중 사용자가 스케줄러(최대 하루 1회)를 기다리지 않고, 이미 등록된 카드로
    // 지금 바로 재시도한다. resume()과 달리 ACTIVE가 아니라 PAST_DUE 대상이고, 새 카드를 등록하는
    // 게 아니라 기존 카드를 그대로 재사용한다.
    @Transactional
    public SubscriptionResponse retryPastDueNow(Long userId) {
        Subscription subscription = paymentService.retryPastDueChargeNow(userId);
        boolean autoRenew = paymentService.hasActiveBillingKey(subscription.getUser());
        return SubscriptionResponse.from(subscription, autoRenew);
    }

    // 해지 예약 취소(자동갱신 재개) - 새 결제 없이 빌링키만 다시 등록한다.
    // 이미 결제한 기간(expiredAt)은 그대로 유지되고, 다음 자동갱신부터 다시 청구된다.
    @Transactional
    public SubscriptionResponse resume(Long userId, String billingKey) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 중복 요청(더블클릭/여러 탭)이 와도 결과가 같도록 attachBillingKey가 멱등하게 처리함
        // (이미 활성 빌링키가 있으면 조용히 넘어감 - cancel()과 대칭되는 설계).
        paymentService.attachBillingKey(subscription.getUser(), billingKey);

        boolean autoRenew = paymentService.hasActiveBillingKey(subscription.getUser());
        return SubscriptionResponse.from(subscription, autoRenew);
    }
}