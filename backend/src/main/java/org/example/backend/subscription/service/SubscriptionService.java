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
        // "해지"는 두 번(중복 클릭/여러 탭, 또는 정말 동시에 도착하는 두 요청) 호출해도 결과가 같아야
        // 하는(멱등) 액션이다. 구독 행에 락을 걸고 조회해서, 동시에 도착한 두 번째 요청은 첫 번째 요청이
        // 커밋될 때까지 여기서 블록됐다가 이미 반영된 최신 상태(cancelRequested=true 등)를 보고
        // 자연스럽게 "이미 해지 처리됨"으로 넘어가게 한다.
        Subscription subscription = subscriptionRepository.findByUserIdAndStatusInForUpdate(userId, LIVE_STATUSES)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            // (설계) 구독을 즉시 CANCELLED로 바꾸지 않고, 빌링키(카드)도 안 지운 채 "해지 예약" 플래그만
            // 세운다. 환불 정책이 없어서 이미 낸 돈만큼(expiredAt까지)은 계속 이용하게 해주는 게 맞고,
            // 카드를 살려두면 만료 전에 마음이 바뀌었을 때 재등록 없이 바로 되돌릴 수 있다(resume() 참고).
            // 이번 결제 기간이 끝나면 PaymentService.attemptRenewalCharge가 이 플래그를 보고 청구 없이
            // 바로 CANCELLED로 확정한다 - 별도 상태 전이 없이 기존 갱신 로직에 분기 하나만 추가한 것.
            if (!subscription.isCancelRequested()) {
                subscription.requestCancel();
                emailService.sendSubscriptionCancelled(subscription.getUser().getUsername());
            }
            // 이미 해지 예약된 상태면 조용히 넘어감 (이메일도 첫 요청 때 이미 보냈음)
        } else {
            // PAST_DUE: 이미 결제가 한 번 실패해서 재시도 중이던 카드라 살려둘 이유가 없다(재개(resume)도
            // 지금 재시도(retryPastDueNow)도 이 카드를 다시 쓰는 경로가 없음 - 둘 다 ACTIVE 전용이거나
            // 별도 재시도 액션이다) - 기존과 동일하게 빌링키를 즉시 정리해 확정 취소한다.
            deleteBillingKeyIfPresent(subscription, "사용자 요청에 의한 구독 해지");
        }

        return SubscriptionResponse.from(subscription, false);
    }

    // 회원 탈퇴 전용 해지. 일반 cancel()과 달리 상태와 무관하게 항상 카드를 즉시 정리한다 - 탈퇴는
    // 재개 가능성이 없으므로, 일반 해지처럼 만료 시점까지 카드를 남겨둘 이유가 없다(불필요하게 오래
    // 들고 있지 않기 위함). UserService.withdrawAccount()가 "정리할 구독이 있는지"를 hasLiveSubscription()
    // 으로 먼저 확인한 뒤에만 부른다.
    @Transactional
    public void cancelForWithdrawal(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatusInForUpdate(userId, LIVE_STATUSES)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        deleteBillingKeyIfPresent(subscription, "회원 탈퇴에 따른 구독 해지");
    }

    // cancel()의 PAST_DUE 분기/cancelForWithdrawal이 공유하는 즉시 정리 로직.
    // (예전엔 PaymentService.deleteBillingKey(User,..)가 빌링키 없음을 예외로 던지고 호출부에서
    // catch해서 판단했는데, 그 호출이 다른 빈(bean)의 @Transactional 경계를 넘는 크로스빈 호출이라
    // Spring이 예외가 나가는 순간 공유 트랜잭션을 rollback-only로 표시해버려 커밋 시점에
    // UnexpectedRollbackException이 나는 문제가 있었음. 판단을 호출자 쪽에서 먼저 끝내 그 문제 자체가
    // 생기지 않게 한다.)
    private void deleteBillingKeyIfPresent(Subscription subscription, String reason) {
        BillingKey billingKey = billingKeyRepository
                .findByUserAndStatusForUpdate(subscription.getUser(), BillingKeyStatus.ACTIVE)
                .orElse(null);

        if (billingKey != null) {
            paymentService.deleteBillingKey(billingKey, reason);
            emailService.sendSubscriptionCancelled(subscription.getUser().getUsername());
        }
        // billingKey가 없으면 이미 해지된 상태 -> 조용히 넘어감 (이메일도 첫 요청 때 이미 보냈음)
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
        return SubscriptionResponse.from(subscription, resolveAutoRenew(subscription));
    }

    // "다음 결제일에 자동 갱신되는지" 판단. 상태별로 근거가 다르다.
    //  - ACTIVE: 이제 카드 유무가 아니라 해지 예약 여부로 판단한다. 해지해도 카드는 만료 시점까지
    //    살려두므로(cancel() 참고), hasActiveBillingKey만으론 더 이상 "갱신될지"를 구분할 수 없다.
    //  - PAST_DUE: 여전히 "재시도할 카드가 남아있는지"가 관심사라 카드 유무를 그대로 쓴다.
    private boolean resolveAutoRenew(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return !subscription.isCancelRequested();
        }
        return paymentService.hasActiveBillingKey(subscription.getUser());
    }

    // 유예기간(PAST_DUE) 중 사용자가 스케줄러(최대 하루 1회)를 기다리지 않고, 이미 등록된 카드로
    // 지금 바로 재시도한다. resume()과 달리 ACTIVE가 아니라 PAST_DUE 대상이고, 새 카드를 등록하는
    // 게 아니라 기존 카드를 그대로 재사용한다.
    @Transactional
    public SubscriptionResponse retryPastDueNow(Long userId) {
        Subscription subscription = paymentService.retryPastDueChargeNow(userId);
        return SubscriptionResponse.from(subscription, resolveAutoRenew(subscription));
    }

    // 해지 예약 취소(자동갱신 재개) - cancel()이 카드를 지우지 않으므로, 이제 새 카드 등록 없이
    // "해지 예약" 플래그만 되돌리면 된다(PortOne 호출 자체가 필요 없음). 이미 결제한 기간(expiredAt)은
    // 그대로 유지되고, 다음 자동갱신부터 이 카드로 다시 청구된다.
    @Transactional
    public SubscriptionResponse resume(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (subscription.isCancelRequested()) {
            // 정상 흐름에서는 cancel()이 카드를 지우지 않으므로 항상 있어야 하지만, 예외적으로 카드가
            // 사라진 경우(PortOne 콘솔에서 수동 삭제 등)를 대비한 방어적 체크.
            if (!paymentService.hasActiveBillingKey(subscription.getUser())) {
                throw new BusinessException(SubscriptionErrorCode.NO_ACTIVE_BILLING_KEY);
            }
            subscription.revokeCancelRequest();
        }
        // 이미 해지 예약 자체를 안 했거나 이미 재개된 상태면 조용히 넘어감 (cancel()과 대칭되는 멱등 처리)

        return SubscriptionResponse.from(subscription, resolveAutoRenew(subscription));
    }

    // 결제수단 변경 준비 - 이미 자동갱신 중(활성 빌링키 있음)인 사용자가 카드를 바꾸고 싶을 때, 새 빌링키
    // 발급에 필요한 값만 내려줌. 아직 자동갱신을 등록한 적 없으면(카드 자체가 없음) NO_ACTIVE_BILLING_KEY로
    // 막는다 - 그 경우는 여기가 아니라 최초 구독(completePayment) 대상이다.
    public PaymentPrepareResponse prepareCardChange(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!paymentService.hasActiveBillingKey(subscription.getUser())) {
            throw new BusinessException(SubscriptionErrorCode.NO_ACTIVE_BILLING_KEY);
        }

        return paymentService.prepareBillingKeyReissue(subscription.getUser(), subscription.getPlanType());
    }

    // 결제수단 변경 - prepareCardChange에서 발급받은 새 billingKey로 기존 카드를 교체한다.
    // resume과 달리 즉시 결제는 없지만 "카드 교체"가 목적이라 기존 활성 빌링키는 유지가 아니라 정리된다
    // (PaymentService.replaceBillingKey 참고).
    @Transactional
    public SubscriptionResponse changeCard(Long userId, String billingKey) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        paymentService.replaceBillingKey(subscription.getUser(), billingKey);

        return SubscriptionResponse.from(subscription, resolveAutoRenew(subscription));
    }
}