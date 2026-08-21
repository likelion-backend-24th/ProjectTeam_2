package org.example.backend.payment.service;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.errors.BillingKeyAlreadyDeletedException;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.payment.*;
import io.portone.sdk.server.payment.billingkey.BillingKeyInfo;
import io.portone.sdk.server.payment.billingkey.IssuedBillingKeyInfo;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.auth.service.EmailService;
import org.example.backend.payment.entity.*;
import org.example.backend.payment.entity.Payment;
import org.example.backend.payment.entity.PaymentStatus;
import org.example.backend.payment.entity.PaymentTransaction;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.repository.PaymentTransactionRepository;
import org.example.backend.payment.repository.WebhookEventRepository;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.user.entity.AccountStatus;
import org.springframework.beans.factory.annotation.Value;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.dto.PaymentPrepareResponse;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PortOneClient portOneClient;
    private final WebhookVerifier webhookVerifier;
    private final WebhookEventRepository webhookEventRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final EmailService emailService;

    @Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.channel-key.billing}")
    private String channelKeyBilling;

    // 빌링키 발급에 필요한 값만 내려줌 (금액이 발생하는 액션이 아니라 PortOne 사전등록/DB 저장 없이 바로 응답)
    public PaymentPrepareResponse preparePayment(User user, SubscriptionPlanType planType) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.USER_INACTIVE);
        }

        if (subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            throw new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        return PaymentPrepareResponse.builder()
                .issueId("p2g-kjs_" + UUID.randomUUID())
                .storeId(storeId)
                .channelKey(channelKeyBilling)
                .amount(planType.getAmount())
                .orderName(planType.getOrderName())
                .build();
    }

    // 최초 구독
    // 프론트가 requestIssueBillingKey로 발급받은 billingKey를 넘겨주면
    // 검증 -> 빌링키 저장 -> 그 빌링키로 첫 결제까지 서버가 직접 수행
    @Transactional
    public void completePayment(User user, String billingKey, SubscriptionPlanType planType) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.USER_INACTIVE);
        }

        if (subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            throw new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        // 유예기간(PAST_DUE) 중이던 예전 구독이 있으면 조용히 정리하고 새로 시작.
        // 안 지우면: 이번에 새로 저장하는 빌링키를 나중에 스케줄러가 "옛날 유예기간 구독" 갱신에도 써버려서
        // 구독이 2개(새로 만든 것 + 옛날 것) 동시에 ACTIVE가 되는 사고가 날 수 있음.
        subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.PAST_DUE)
                .ifPresent(Subscription::cancel);

        // PortOne에 실제로 발급된 빌링키가 맞는지, 우리 상점/채널의 빌링키가 맞는지 조회 및 검증
        BillingKeyInfo billingKeyInfo = portOneClient.getPayment().getBillingKey().getBillingKeyInfo(billingKey).join();

        if (!(billingKeyInfo instanceof IssuedBillingKeyInfo issue)
                || !issue.getStoreId().equals(storeId)
                || issue.getChannels().stream().noneMatch(c -> c.getKey().equals(channelKeyBilling))) {
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        }

        // 이전에 남아있던 활성 빌링키가 있으면 정리하고 새로 저장 (결제 실패로 끊긴 경우 등)
        billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)
                .ifPresent(old -> {
                    old.setStatus(BillingKeyStatus.DELETED);
                    old.setDeletedAt(LocalDateTime.now());
                });
        billingKeyRepository.save(new BillingKey(user, billingKey));

        // 저장된 빌링키로 첫 결제 시도
        String paymentId = "p2g-kjs_" + UUID.randomUUID();
        int amount = planType.getAmount();
        Payment payment = new Payment(paymentId, user, planType, amount);
        paymentRepository.save(payment);

        try {
            portOneClient.getPayment().payWithBillingKey(
                    paymentId, billingKey, channelKeyBilling, planType.getOrderName(),
                    null, null,
                    new PaymentAmountInput(amount, null, null), Currency.Krw.INSTANCE,
                    null, null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null
            ).join();
        } catch (RuntimeException e) {
            markPaymentFailed(payment);
            Throwable cause = e.getCause() != null ? e.getCause() : e; // .join()이 CompletionException으로 감싸므로 원인 예외를 꺼냄
            log.warn("빌링키 결제 실패 (paymentId={}, reason={})", paymentId, cause.getMessage());
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        verifyAndFinalize(payment);

    }

    // 스케줄러가 만료된 ACTIVE 구독을 넘겨주면 첫 재결제를 시도.
    @Transactional
    public void renewSubscription(Subscription detachedSubscription) {
        // 스케줄러가 트랜잭션 밖에서 조회해 넘겨준 detached 엔티티라 그대로 쓰면 안 됨.
        // (1) subscription.getUser() 접근 시 LazyInitializationException 남
        // (2) 설령 안 터지더라도 detached 엔티티 수정은 이 트랜잭션에 반영이 안 됨(Hibernate가 추적을 안 함)
        // 그래서 이 트랜잭션 안에서 다시 조회해 managed 상태로 만든 뒤 사용해야 실제로 DB에 반영됨.
        Subscription subscription = subscriptionRepository.findById(detachedSubscription.getId())
                .orElseThrow(() -> new IllegalStateException("구독을 찾을 수 없음: id=" + detachedSubscription.getId()));
        attemptRenewalCharge(subscription);
    }

    // 스케줄러가 PAST_DUE(유예기간 중) 구독을 넘겨주면, 유예기간이 끝났는지/오늘 이미 시도했는지 확인 후
    // 필요하면 재시도하거나 최종 취소한다. 하루에 한 번만 시도하도록 lastRetryAt으로 걸러냄.
    @Transactional
    public void processPastDueSubscription(Subscription detachedSubscription) {
        Subscription subscription = subscriptionRepository.findById(detachedSubscription.getId())
                .orElseThrow(() -> new IllegalStateException("구독을 찾을 수 없음: id=" + detachedSubscription.getId()));

        if (subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            return; // 그 사이 다른 경로로 이미 처리된 경우 대비
        }

        // 유예기간(만료일+3일)이 이미 지났으면 더 시도하지 않고 바로 최종 취소
        if (!LocalDateTime.now().isBefore(subscription.getGraceEndsAt())) {
            finalizeCancellation(subscription, subscription.getUser());
            return;
        }

        // 오늘 이미 재시도했으면 스킵 (하루 한 번만)
        if (subscription.getLastRetryAt() != null
                && subscription.getLastRetryAt().toLocalDate().isEqual(LocalDateTime.now().toLocalDate())) {
            return;
        }

        attemptRenewalCharge(subscription);
    }

    // renewSubscription과 processPastDueSubscription이 공유하는 실제 결제 시도 로직.
    // 빌링키로 결제해보고, 성공/실패에 따라 이후 상태 전환은 verifyAndFinalize / markPaymentFailed가 맡는다.
    private void attemptRenewalCharge(Subscription subscription) {
        User user = subscription.getUser();

        BillingKey billingKey = billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE).orElse(null);
        if (billingKey == null) {
            // 빌링키가 아예 없으면 재시도해봤자 의미가 없으니 유예기간 없이 바로 최종 취소
            log.info("갱신 실패 - 활성 빌링키 없음 (subscriptionId={})", subscription.getId());
            finalizeCancellation(subscription, user);
            return;
        }

        SubscriptionPlanType planType = subscription.getPlanType();
        String paymentId = "p2g-kjs_" + UUID.randomUUID();
        int amount = planType.getAmount();
        Payment payment = new Payment(paymentId, user, planType, amount);
        payment.setSubscription(subscription);
        paymentRepository.save(payment);

        try {
            portOneClient.getPayment().payWithBillingKey(
                    paymentId, billingKey.getBillingKey(), channelKeyBilling, planType.getOrderName(),
                    null, null,
                    new PaymentAmountInput(amount, null, null), Currency.Krw.INSTANCE,
                    null, null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null
            ).join();
        } catch (RuntimeException e) {
            markPaymentFailed(payment);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("갱신 결제 실패 (paymentId={}, reason={})", paymentId, cause.getMessage());
            return;
        }

        try {
            verifyAndFinalize(payment);
        } catch (BusinessException e) {
            log.warn("갱신 결제 검증 실패 (paymentId={}, reason={})", paymentId, e.getMessage());
        }
    }

    @Transactional
    public void deleteBillingKey(User user, String reason) {
        BillingKey billingKey = billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_NOT_FOUND));

        try {
            portOneClient.getPayment().getBillingKey().deleteBillingKey(billingKey.getBillingKey(), reason, null, null).join();
        } catch (RuntimeException e) {
            // PortOne 쪽에 이미 삭제된 빌링키면(관리자 콘솔에서 미리 지웠거나 중복 요청 등)
            // 원하는 상태(빌링키 없음)는 이미 달성된 거라 에러가 아니라 성공으로 취급함.
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (!(cause instanceof BillingKeyAlreadyDeletedException)) {
                throw new BusinessException(PaymentErrorCode.BILLING_KEY_DELETE_FAILED);
            }
        }

        billingKey.setStatus(BillingKeyStatus.DELETED);
        billingKey.setDeletedAt(LocalDateTime.now());
    }

    // 활성 빌링키(=다음 자동갱신 예정) 여부. 프론트에 "해지 예약" 상태를 보여줄 때 씀.
    public boolean hasActiveBillingKey(User user) {
        return billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE).isPresent();
    }

    // 웹훅
    @Transactional
    public void handleWebhook(String body, String webhookId, String signature, String timestamp) {
        Webhook webhook;
        try {
            // 서명 검증
            webhook = webhookVerifier.verify(body, webhookId, signature, timestamp);
        } catch(WebhookVerificationException e){
            throw new BusinessException(PaymentErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }

        if (webhookEventRepository.existsByWebhookId(webhookId)) {
            log.info("이미 처리한 웹훅이라 스킵 (webhookId={})", webhookId);
            return;
        }
        webhookEventRepository.save(new WebhookEvent(webhookId));

        if (!(webhook instanceof WebhookTransaction transaction)) {
            return;
        }

        String paymentId = transaction.getData().getPaymentId();
        paymentRepository.findByPaymentId(paymentId).ifPresent(payment -> {
            try {
                verifyAndFinalize(payment);
            } catch (BusinessException e) {
                log.warn("웹훅 처리 중 결제 검증 실패 (paymentId={}, reason={})", paymentId, e.getMessage());
            }
        });
    }

    // 결제 실패 시 공통 처리: Payment를 FAILED로 바꾸고, 갱신 시도였다면(payment.getSubscription() != null) 구독도 처리.
    // completePayment(신규)에서 실패하면 payment.getSubscription()이 애초에 null이라 이 분기 자체가 스킵됨.
    //
    // 갱신 실패는 두 갈래로 나뉜다:
    //  - 지금 ACTIVE인 구독의 첫 실패(만료일에 막 걸림) -> 바로 취소하지 않고 유예기간(PAST_DUE) 시작
    //  - 이미 PAST_DUE인 구독의 재시도 실패 -> 유예기간이 아직 남았으면 "오늘 시도했다"만 기록(상태 유지)
    //    (유예기간이 끝났는지 여부는 processPastDueSubscription이 재시도 전에 미리 걸러주므로 여기선 안 봐도 됨)
    private void markPaymentFailed(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        Subscription subscription = payment.getSubscription();
        if (subscription == null) {
            return;
        }

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            subscription.recordRetryAttempt();
        } else {
            subscription.markPastDue(subscription.getExpiredAt().plusDays(3));
            payment.getUser().setSubscribed(false);
        }
    }

    // 유예기간을 다 썼거나(재시도 소진) 애초에 빌링키가 없을 때의 최종 취소 처리.
    // 구독 취소 + 접근 권한 해제 + 알림 메일 + 빌링키 로컬 정리까지 한 번에.
    private void finalizeCancellation(Subscription subscription, User user) {
        subscription.cancel();
        user.setSubscribed(false);
        emailService.sendSubscriptionRenewalFailed(user.getUsername());

        billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)
                .ifPresent(billingKey -> {
                    billingKey.setStatus(BillingKeyStatus.DELETED);
                    billingKey.setDeletedAt(LocalDateTime.now());
                });
    }

    @Transactional
    public void verifyAndFinalize(Payment payment) {

        // 이미 PAID(완료)면 조용히 종료
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        // 포트원에 실제 결제 조회
        io.portone.sdk.server.payment.Payment portOnePayment =
                portOneClient.getPayment().getPayment(payment.getPaymentId()).join();

        // 아직 결제 진행 중(Ready/PayPending)이면 판단 보류 - 결제 완료 전 웹훅이 먼저 도착하는 경우 대비
        if (portOnePayment instanceof ReadyPayment || portOnePayment instanceof PayPendingPayment) {
            return;
        }

        // 조회 결과가 '결제 실패' 타입이면 failed 변수로 실패 정보를 담음
        if (portOnePayment instanceof FailedPayment failed) {
            // Payment 상태를 FAILED로 바꾸고
            markPaymentFailed(payment);
            // 해당 트랜잭션ID가 이미 존재하는지 확인 후
            if (!paymentTransactionRepository.existsByTransactionId(failed.getTransactionId())){
                // 실패한 거래 ID(failed.getTransactionId())를 꺼내서 PatmentTransaction 기록을 남김
                paymentTransactionRepository.save(new PaymentTransaction(
                        payment, failed.getTransactionId(), PaymentTransactionStatus.FAILED,
                        "PortOne 결제 실패"));
            }
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 우리가 사용하지 않는 Payment 타입이 들어왔을 때를 대비한 방어코드
        if (!(portOnePayment instanceof PaidPayment paid)) {
            markPaymentFailed(payment);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 이 결제가 우리 상점 결제가 맞는지,
        // prepare에서 지정한 결제 채널로 결제된 게 맞는지,
        // 포트원에서 실제로 승인한 금액과 우리 DB에 저장한 금액이 같은지,
        // 통화가 원화(KRW)가 맞는지 확인
        boolean valid = paid.getStoreId().equals(storeId)
                && paid.getChannel().getKey().equals(channelKeyBilling)
                && paid.getAmount().getTotal() == payment.getAmount()
                && paid.getCurrency() instanceof Currency.Krw;

        // 위 조건 중 하나라도 맞지 않다면 FAILED
        if (!valid) {
            markPaymentFailed(payment);
            // 해당 트랜잭션ID가 이미 존재하는지 확인 후
            if (!paymentTransactionRepository.existsByTransactionId(paid.getTransactionId())) {
                paymentTransactionRepository.save(new PaymentTransaction(
                        payment, paid.getTransactionId(), PaymentTransactionStatus.FAILED,
                        "저장된 결제 정보와 불일치"));
            }
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 성공 후 로직
        if (paymentTransactionRepository.existsByTransactionId(paid.getTransactionId())) {
            return; // 동시 요청(웹훅/완료API)으로 이미 다른 호출이 이 거래를 기록/반영함
        }

        payment.setStatus(PaymentStatus.PAID);

        // 성공 거래 기록
        PaymentTransaction tx = new PaymentTransaction(payment, paid.getTransactionId(),
                PaymentTransactionStatus.SUCCEEDED, null);
        tx.setOccurredAt(LocalDateTime.ofInstant(paid.getPaidAt(), ZoneId.systemDefault()));
        paymentTransactionRepository.save(tx);

        // 결제 성공 = 갱신이면 기존 구독 연장, 신규면 구독 생성.
        // (설계) 신규/갱신을 별도 파라미터나 메서드로 안 나누고, payment.getSubscription()이 미리 세팅돼 있는지로 구분함.
        //   - completePayment: Payment 생성 시 subscription을 안 세팅 -> 여기서 새로 만듦
        //   - renewSubscription: Payment 생성 시 갱신 대상 subscription을 미리 세팅해둠 -> 여기서 연장만 함
        // 이렇게 하면 검증 로직(포트원 재조회, 상점/채널/금액 확인, 중복 방지 등)을 신규/갱신 양쪽에서 통째로 재사용할 수 있음.
        if (payment.getSubscription() != null) {
            Subscription subscription = payment.getSubscription();
            if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
                // 유예기간 중 재결제 성공 -> "낸 만큼 정확히 쓴다" 원칙으로 지금 시점부터 한 달
                subscription.recoverFromPastDue();
                payment.getUser().setSubscribed(true);
            } else {
                // 정상 주기 갱신 -> 원래 만료일 기준으로 한 달 연장 (접근 끊긴 적 없으니 그대로)
                subscription.extend();
            }
        } else {
            Subscription subscription = Subscription.builder()
                    .user(payment.getUser())
                    .planType(payment.getPlanType())
                    .startedAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusMonths(1))
                    .build();
            subscriptionRepository.save(subscription);

            payment.setSubscription(subscription);
            payment.getUser().setSubscribed(true);
            emailService.sendSubscriptionStarted(payment.getUser().getUsername()); // 최초 구독만 해당, 갱신(extend)은 스킵
        }
    }
}
