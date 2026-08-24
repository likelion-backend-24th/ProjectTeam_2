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
import org.example.backend.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    // retryPastDueChargeNow(수동 재시도)의 연타 방지 쿨다운. 값 자체보다 "방금 시도했으면 잠깐 막는다"가
    // 목적이라 정밀하게 튜닝할 값은 아님.
    private static final long RETRY_COOLDOWN_SECONDS = 60;

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

        return buildBillingKeyIssueParams(planType);
    }

    // 해지 예약 취소(자동갱신 재개) 시 새 빌링키 발급에 필요한 값만 내려줌.
    // preparePayment와 다르게 "ACTIVE 구독 없음"을 요구하지 않음 - 여기선 반대로 ACTIVE 구독이
    // 있어야 정상 케이스(해지 예약된 구독을 되살리는 것)이고, 그 상태 검증은 호출자(SubscriptionService)가 한다.
    public PaymentPrepareResponse prepareBillingKeyReissue(User user, SubscriptionPlanType planType) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.USER_INACTIVE);
        }

        if (planType == null) {
            // planType 컬럼은 나중에 추가된 필드라, 이 컬럼이 생기기 전부터 있던 레거시 ACTIVE 구독
            // 행은 NULL일 수 있다(ddl-auto: update는 NOT NULL 제약을 소급 적용/백필해주지 않음).
            // attemptRenewalCharge와 동일하게 BASIC으로 보정해서 진행하되, 로그로 남긴다.
            log.error("구독의 planType이 NULL - 레거시 데이터로 추정, BASIC으로 보정 후 진행 (userId={})", user.getId());
            planType = SubscriptionPlanType.BASIC;
        }

        return buildBillingKeyIssueParams(planType);
    }

    private PaymentPrepareResponse buildBillingKeyIssueParams(SubscriptionPlanType planType) {
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
    // noRollbackFor = BusinessException.class:
    // verifyAndFinalize가 검증 실패로 BusinessException을 던지면 프론트엔 여전히 실패로 전달돼야 하지만,
    // 이미 실제로는 PortOne 쪽에서 카드 결제가 일어난 뒤라(=롤백 불가능한 외부 상태) 그 직전까지 저장한
    // 빌링키/Payment/실패 기록(PaymentTransaction)까지 같이 롤백되면 결제는 됐는데 우리 DB엔 흔적이
    // 하나도 안 남는 사고가 남. 그래서 이 예외 한정으로는 롤백하지 않고 지금까지의 기록은 커밋되게 함.
    @Transactional(noRollbackFor = BusinessException.class)
    public void completePayment(User user, String billingKey, SubscriptionPlanType planType) {
        // 동시에 두 번(더블클릭, 중복 요청 등) 결제 요청이 들어와도 순서대로 처리되도록 유저 행에 락을 검.
        // 안 걸면 두 요청이 동시에 아래 "ACTIVE 구독 없음" 체크를 통과해서 이중결제/이중구독이 날 수 있음.
        // (문의 스레드 생성 시 동시성 제어와 동일한 패턴 재사용)
        userRepository.findByIdForUpdate(user.getId());

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.USER_INACTIVE);
        }

        if (subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            throw new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        // PortOne에 실제로 발급된 빌링키가 맞는지, 우리 상점/채널의 빌링키가 맞는지 조회 및 검증.
        // 이 메서드는 @Transactional(noRollbackFor = BusinessException.class)라 여기서 BusinessException을
        // 던져도 그 이후의 변경은 롤백 안 됨 - 그래서 되돌릴 수 없는 상태 변경(PAST_DUE 정리 등)은
        // 전부 "새 구독이 실제로 만들어졌다"가 확정된 뒤(맨 아래 verifyAndFinalize 성공 이후)로 미룬다.
        // 검증/청구/검증재확인 중 어디서 실패하든 여기까지 아무것도 안 바뀐 채로 예외만 던져야 한다.
        // (안 그러면, 예를 들어 유예기간 중이던 사용자가 재구독을 시도했다가 카드 승인이 거절된 것뿐인데도
        // noRollbackFor 때문에 옛날 PAST_DUE 구독이 영구 취소되는 사고가 남 - 재시도 기회를 뺏는 셈)
        verifyBillingKeyOwnership(billingKey);

        // 이전에 남아있던 활성 빌링키가 있으면 정리하고 새로 저장 (결제 실패로 끊긴 경우 등)
        replaceActiveBillingKey(user, billingKey, "새 빌링키로 교체");

        // 저장된 빌링키로 첫 결제 시도
        Payment payment = chargeWithBillingKey(user, billingKey, planType, null, "빌링키 결제 실패");
        if (payment == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        verifyAndFinalize(payment);

        // 유예기간(PAST_DUE) 중이던 예전 구독이 있으면 조용히 정리.
        // 안 지우면: 방금 저장한 빌링키를 나중에 스케줄러가 "옛날 유예기간 구독" 갱신에도 써버려서
        // 구독이 2개(방금 만든 것 + 옛날 것) 동시에 ACTIVE가 되는 사고가 날 수 있음.
        // 위의 verifyAndFinalize가 예외 없이 끝난 뒤에만 실행되므로, 청구/검증이 도중에 실패하면
        // (noRollbackFor라도) 여기까지 도달하지 못해 옛날 구독은 그대로 PAST_DUE로 남아 재시도 기회가 유지된다.
        subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.PAST_DUE)
                .ifPresent(Subscription::cancel);
    }

    // 스케줄러가 만료된 ACTIVE 구독을 넘겨주면 첫 재결제를 시도.
    @Transactional
    public void renewSubscription(Subscription detachedSubscription) {
        // 스케줄러가 트랜잭션 밖에서 조회해 넘겨준 detached 엔티티라 그대로 쓰면 안 됨.
        // (1) subscription.getUser() 접근 시 LazyInitializationException 남
        // (2) 설령 안 터지더라도 detached 엔티티 수정은 이 트랜잭션에 반영이 안 됨(Hibernate가 추적을 안 함)
        // 그래서 이 트랜잭션 안에서 다시 조회해 managed 상태로 만든 뒤 사용해야 실제로 DB에 반영됨.
        //
        // findById가 아니라 findByIdForUpdate(락 조회)를 쓰는 이유: 이게 이 트랜잭션에서 이 구독을
        // "제일 처음" 읽는 지점이어야 락이 의미가 있다(SubscriptionRepository.findByIdForUpdate 주석
        // 참고). 락을 걸어야 이 갱신 시도와 사용자의 수동 재시도(retryPastDueChargeNow)가 같은
        // 구독에 동시에 들어와도 순서대로 처리되고, 뒤에 처리되는 쪽은 앞쪽이 이미 상태를 바꿔놓은
        // 걸 정확히 보고 중복 청구하지 않을 수 있다.
        Subscription subscription = subscriptionRepository.findByIdForUpdate(detachedSubscription.getId())
                .orElseThrow(() -> new IllegalStateException("구독을 찾을 수 없음: id=" + detachedSubscription.getId()));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return; // 락을 기다리는 동안 다른 동시 처리가 이미 상태를 바꿔놓은 경우 - 중복 청구 방지
        }

        attemptRenewalCharge(subscription);
    }

    // 스케줄러가 PAST_DUE(유예기간 중) 구독을 넘겨주면, 유예기간이 끝났는지/오늘 이미 시도했는지 확인 후
    // 필요하면 재시도하거나 최종 취소한다. 하루에 한 번만 시도하도록 lastRetryAt으로 걸러냄.
    @Transactional
    public void processPastDueSubscription(Subscription detachedSubscription) {
        // findByIdForUpdate(락 조회)를 쓰는 이유는 renewSubscription과 동일 - 이 갱신 시도와
        // 사용자의 수동 재시도가 동시에 들어와도 순서대로 처리되게 하기 위함.
        Subscription subscription = subscriptionRepository.findByIdForUpdate(detachedSubscription.getId())
                .orElseThrow(() -> new IllegalStateException("구독을 찾을 수 없음: id=" + detachedSubscription.getId()));

        if (subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            return; // 그 사이 다른 경로로 이미 처리된 경우 대비 (락을 기다리다 뒤늦게 여기 온 경우 포함)
        }

        // 유예기간(만료일+3일)이 이미 지났으면 더 시도하지 않고 바로 최종 취소
        if (!LocalDateTime.now().isBefore(subscription.getGraceEndsAt())) {
            finalizeCancellation(subscription, subscription.getUser(), true); // 진짜 결제 실패로 끝난 경우 - 안내 메일 보냄
            return;
        }

        // 오늘 이미 재시도했으면 스킵 (하루 한 번만)
        if (subscription.getLastRetryAt() != null
                && subscription.getLastRetryAt().toLocalDate().isEqual(LocalDateTime.now().toLocalDate())) {
            return;
        }

        attemptRenewalCharge(subscription);
    }

    // 유예기간(PAST_DUE) 중 사용자가 스케줄러(최대 하루 1회)를 기다리지 않고, 이미 등록된 카드로
    // 지금 바로 재시도하고 싶을 때 쓴다. 카드를 새로 등록하지 않고 기존 활성 빌링키를 그대로
    // 재사용한다는 점에서, 카드를 다시 등록하며 완전히 새 구독을 만드는 completePayment와 다르다.
    // processPastDueSubscription과 달리 "오늘 이미 시도했는지"(하루 단위)는 안 본다 - 사용자가 직접
    // 누른 명시적 요청이라 하루 1회 제한을 걸 이유는 없지만, 대신 아래 RETRY_COOLDOWN으로 아주
    // 짧은 연타(새로고침 후 다시 클릭, 두 탭에서 거의 동시 클릭 등)만 막는다.
    @Transactional
    public Subscription retryPastDueChargeNow(Long userId) {
        // findByUserIdAndStatusForUpdate(락 조회)를 쓰는 이유는 renewSubscription/
        // processPastDueSubscription과 동일 - 이 수동 재시도와 스케줄러(또는 다른 탭의 수동 재시도)가
        // 같은 구독에 동시에 들어와도 순서대로 처리되고, 뒤에 처리되는 쪽은 앞쪽이 이미 바꿔놓은
        // 최신 상태를 정확히 보게 하기 위함 (SubscriptionRepository.findByIdForUpdate 주석 참고).
        Subscription subscription = subscriptionRepository
                .findByUserIdAndStatusForUpdate(userId, SubscriptionStatus.PAST_DUE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 유예기간이 이미 끝났으면 재시도해봤자 의미 없음 - 곧 스케줄러가 최종 취소로 정리할 것이므로
        if (!LocalDateTime.now().isBefore(subscription.getGraceEndsAt())) {
            throw new BusinessException(SubscriptionErrorCode.GRACE_PERIOD_ENDED);
        }

        // 연타 방지: 실패든 성공이든 시도할 때마다 lastRetryAt이 지금 시각으로 갱신되므로(markPastDue/
        // recordRetryAttempt 참고), 방금 시도한 지 얼마 안 됐으면 또 시도하지 않고 막는다.
        // (위 락 덕분에 두 요청이 동시에 여기 도달할 수는 없지만, 순서대로는 둘 다 통과할 수 있어서
        // - 락은 "동시 실행"만 막지 "짧은 시간 내 연속 실행" 자체를 막지는 않음 - 이 체크가 필요함)
        if (subscription.getLastRetryAt() != null
                && subscription.getLastRetryAt().isAfter(LocalDateTime.now().minusSeconds(RETRY_COOLDOWN_SECONDS))) {
            throw new BusinessException(SubscriptionErrorCode.RETRY_TOO_SOON);
        }

        attemptRenewalCharge(subscription); // 성공/실패에 따른 상태 전환은 이 안에서 다 처리됨
        return subscription; // 같은 트랜잭션 안이라 attemptRenewalCharge가 바꾼 상태가 그대로 반영돼 있음
    }

    // renewSubscription/processPastDueSubscription(스케줄러)과 retryPastDueChargeNow(사용자 수동
    // 재시도)가 공유하는 실제 결제 시도 로직. 빌링키로 결제해보고, 성공/실패에 따라 이후 상태 전환은
    // verifyAndFinalize / markPaymentFailed가 맡는다.
    private void attemptRenewalCharge(Subscription subscription) {
        User user = subscription.getUser();

        // 동시성 가드: 스케줄러(하루 1회)와 사용자의 수동 재시도(retryPastDueChargeNow)가 같은
        // 유저에 대해 거의 동시에 들어와도 순서대로 처리되게 유저 행에 락을 검. 안 걸면 두 시도가
        // 동시에 같은 빌링키로 이중 결제를 시도할 수 있음 (completePayment/replaceBillingKey와 동일한 이유).
        userRepository.findByIdForUpdate(user.getId());

        // 해지 예약된 구독이 만료 시점에 도달한 경우 - 카드는 아직 살아있지만(해지 시 안 지움) 청구하지
        // 않고 바로 확정 취소한다. cancelRequested는 ACTIVE 상태에서만 세팅되므로(SubscriptionService.
        // cancel() 참고), PAST_DUE 대상으로 이 메서드를 부르는 processPastDueSubscription/
        // retryPastDueChargeNow 경로에서는 항상 false라 이 분기에 걸릴 일이 없다.
        if (subscription.isCancelRequested()) {
            log.info("갱신 시점에 해지 예약 확인 - 청구 없이 확정 취소 (subscriptionId={})", subscription.getId());
            finalizeCancellation(subscription, user, false);
            return;
        }

        BillingKey billingKey = billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE).orElse(null);
        if (billingKey == null) {
            // 빌링키가 아예 없으면 재시도해봤자 의미가 없으니 유예기간 없이 바로 최종 취소.
            // ACTIVE 중 해지한 경우는 위 cancelRequested 분기에서 이미 걸러지므로, 여기 도달하는 건
            // PAST_DUE 중 해지(카드를 즉시 지움 - SubscriptionService.cancel() 참고)나 그 밖의 예외적으로
            // 카드가 사라진 경우뿐이다. 어느 쪽이든 "결제 실패" 메일이 아니라 cancel() 시점에 이미 보낸
            // "해지 완료" 메일로 충분함 -> 메일 재발송 안 함
            log.info("갱신 실패 - 활성 빌링키 없음 (subscriptionId={})", subscription.getId());
            finalizeCancellation(subscription, user, false);
            return;
        }

        SubscriptionPlanType planType = subscription.getPlanType();
        if (planType == null) {
            // planType 컬럼은 나중에 추가된 필드라, 이 컬럼이 생기기 전부터 있던 레거시 ACTIVE 구독
            // 행은 NULL일 수 있다(ddl-auto: update는 NOT NULL 제약을 소급 적용/백필해주지 않음).
            // 이걸 그냥 두면 바로 아래 chargeWithBillingKey()의 planType.getAmount()에서 NPE가 나는데,
            // 이 메서드를 부르는 스케줄러 쪽 catch(RuntimeException)가 조용히 삼켜버려서 이 구독은
            // 청구도 PAST_DUE 전환도 안 된 채 영원히 ACTIVE로 남는 사고가 난다.
            // 지금은 플랜이 BASIC 하나뿐이라 안전하게 기본값으로 보정해서 정상 흐름은 이어가되,
            // 이런 레거시 데이터가 실제로 있었다는 걸 알아채고 DB에서 직접 백필할 수 있게 크게 로그를 남긴다.
            log.error("구독의 planType이 NULL - 레거시 데이터로 추정, BASIC으로 보정 후 갱신 진행 (subscriptionId={})",
                    subscription.getId());
            planType = SubscriptionPlanType.BASIC;
        }
        Payment payment = chargeWithBillingKey(user, billingKey.getBillingKey(), planType, subscription, "갱신 결제 실패");
        if (payment == null) {
            return;
        }

        try {
            verifyAndFinalize(payment);
        } catch (BusinessException e) {
            log.warn("갱신 결제 검증 실패 (paymentId={}, reason={})", payment.getPaymentId(), e.getMessage());
        }
    }

    // completePayment(신규결제)/attemptRenewalCharge(갱신)가 공유하는 빌링키 결제 요청 로직.
    // PortOne SDK 시그니처가 바뀌면(0.12.0 -> 0.24.0 업그레이드 때도 있었음) 한쪽만 고치고 다른 쪽을
    // 놓치는 실수를 막기 위해 한 곳으로 모음.
    // Payment 생성/저장까지 여기서 처리하고, 결제 요청 자체가 실패하면 markPaymentFailed로 정리 +
    // 로그만 남긴 뒤 null을 돌려준다. 실패 시 예외를 던질지/조용히 넘어갈지는 호출자가 정한다.
    private Payment chargeWithBillingKey(User user, String billingKeyValue, SubscriptionPlanType planType,
                                          Subscription subscriptionOrNull, String failureLogMessage) {
        String paymentId = "p2g-kjs_" + UUID.randomUUID();
        int amount = planType.getAmount();
        Payment payment = new Payment(paymentId, user, planType, amount);
        if (subscriptionOrNull != null) {
            payment.setSubscription(subscriptionOrNull);
        }
        paymentRepository.save(payment);

        try {
            portOneClient.getPayment().payWithBillingKey(
                    paymentId, billingKeyValue, channelKeyBilling, planType.getOrderName(),
                    null, null,
                    new PaymentAmountInput(amount, null, null), Currency.Krw.INSTANCE,
                    null, null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null
            ).join();
        } catch (RuntimeException e) {
            markPaymentFailed(payment);
            Throwable cause = e.getCause() != null ? e.getCause() : e; // .join()이 CompletionException으로 감싸므로 원인 예외를 꺼냄
            log.warn("{} (paymentId={}, reason={})", failureLogMessage, paymentId, cause.getMessage());
            return null;
        }

        return payment;
    }

    // 이미 조회(가급적 락까지 걸어 조회)된 활성 빌링키를 넘겨받아 PortOne 삭제 + 로컬 DELETED 처리만 한다.
    // "활성 빌링키가 있는지"는 호출자가 먼저 판단하게 해서(SubscriptionService.cancel() 참고), 여기선
    // "없음"을 예외로 알릴 필요가 없다. (예전엔 User만 받아서 여기서 직접 조회 후 없으면 예외를 던졌는데,
    // 그 예외를 호출자가 다른 빈(bean)의 @Transactional 경계 너머에서 catch-and-continue로 처리하면서
    // Spring이 공유 트랜잭션을 rollback-only로 표시해버려 커밋 시점에 UnexpectedRollbackException이
    // 나는 문제가 있었음. 판단을 호출자 쪽으로 옮겨서 이 메서드가 "실패할 수 있는 조회"를 아예 안 하게 함)
    @Transactional
    public void deleteBillingKey(BillingKey billingKey, String reason) {
        requestPortOneBillingKeyDeletion(billingKey.getBillingKey(), reason);

        billingKey.setStatus(BillingKeyStatus.DELETED);
        billingKey.setDeletedAt(LocalDateTime.now());
    }

    // PortOne에 실제 빌링키 삭제를 요청한다. 실패하면 예외를 던지므로, 실패해도 흐름을 막고 싶지
    // 않은 호출부는 감싸서 처리한다(cleanUpBillingKey 참고).
    private void requestPortOneBillingKeyDeletion(String billingKey, String reason) {
        try {
            portOneClient.getPayment().getBillingKey().deleteBillingKey(billingKey, reason, null, null).join();
        } catch (RuntimeException e) {
            // PortOne 쪽에 이미 삭제된 빌링키면(관리자 콘솔에서 미리 지웠거나 중복 요청 등)
            // 원하는 상태(빌링키 없음)는 이미 달성된 거라 에러가 아니라 성공으로 취급함.
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (!(cause instanceof BillingKeyAlreadyDeletedException)) {
                throw new BusinessException(PaymentErrorCode.BILLING_KEY_DELETE_FAILED);
            }
        }
    }

    // completePayment(빌링키 교체)/finalizeCancellation(구독 확정 취소)처럼 결제/구독 처리 자체는
    // 이미 끝난 상황에서 뒷정리로 예전 빌링키를 지우는 곳들이 공유하는 헬퍼.
    // PortOne 호출이 실패해도 예외를 던져 이미 끝난 핵심 흐름을 막지 않고 로그만 남긴다.
    // (로컬은 DELETED로 비활성화되어 재사용되지 않으니 실질 위험은 낮음. PortOne 콘솔에서 수동 확인/정리 가능)
    private void cleanUpBillingKey(BillingKey billingKey, String reason) {
        try {
            requestPortOneBillingKeyDeletion(billingKey.getBillingKey(), reason);
        } catch (BusinessException e) {
            log.warn("PortOne 빌링키 삭제 실패 - 로컬은 DELETED로 처리하되 PortOne 콘솔에서 수동 확인 필요 (billingKeyId={}, reason={})",
                    billingKey.getId(), reason);
        }

        billingKey.setStatus(BillingKeyStatus.DELETED);
        billingKey.setDeletedAt(LocalDateTime.now());
    }

    // 활성 빌링키(=청구 가능한 카드가 등록돼 있는지) 여부. PAST_DUE의 "재시도할 카드가 있는지"
    // 판단이나 카드 변경 전제조건 체크에 씀 - ACTIVE의 "다음에 갱신될지"는 이제 이 값이 아니라
    // Subscription.cancelRequested로 판단한다(해지해도 만료 전까지는 카드를 살려두므로).
    public boolean hasActiveBillingKey(User user) {
        return billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE).isPresent();
    }

    // 기존 활성 빌링키가 있으면 정리하고 새 빌링키로 교체 저장한다.
    // completePayment(재구독 시 예전 빌링키 정리)와 replaceBillingKey(결제수단 변경)가 공유하는 헬퍼.
    private void replaceActiveBillingKey(User user, String newBillingKey, String cleanupReason) {
        billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)
                .ifPresent(old -> cleanUpBillingKey(old, cleanupReason));
        billingKeyRepository.save(new BillingKey(user, newBillingKey));
    }

    // PortOne에 실제로 발급된 빌링키가 맞는지, 우리 상점/채널의 빌링키가 맞는지 검증.
    // completePayment/replaceBillingKey가 공유 (원래 completePayment 안에 있던 로직을 그대로 분리한 것).
    private void verifyBillingKeyOwnership(String billingKey) {
        BillingKeyInfo billingKeyInfo = portOneClient.getPayment().getBillingKey().getBillingKeyInfo(billingKey).join();

        if (!(billingKeyInfo instanceof IssuedBillingKeyInfo issue)
                || !issue.getStoreId().equals(storeId)
                || issue.getChannels().stream().noneMatch(c -> c.getKey().equals(channelKeyBilling))) {
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        }
    }

    // 결제수단 변경 - 이미 자동갱신 중(활성 빌링키 있음)인 구독자가 카드를 교체한다.
    // completePayment와 달리 즉시 결제는 발생하지 않고, 다음 정상 갱신부터 새 카드로 청구된다.
    // (해지 예약 취소(SubscriptionService.resume())는 더 이상 이 메서드를 쓰지 않는다 - 해지 시 카드를
    // 지우지 않으므로 재개는 SubscriptionResponse 플래그만 되돌리면 되고, 새 카드 등록 자체가 필요 없다.)
    @Transactional
    public void replaceBillingKey(User user, String newBillingKey) {
        // completePayment와 동일한 이유로 동시성 가드 - 중복 요청(더블클릭/여러 탭)이
        // 동시에 들어와도 순서대로 처리되게 함.
        userRepository.findByIdForUpdate(user.getId());

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.USER_INACTIVE);
        }

        // PortOne에 실제로 발급된 우리 상점 키가 맞는지 재확인 (프론트 응답은 단서일 뿐)
        verifyBillingKeyOwnership(newBillingKey);
        replaceActiveBillingKey(user, newBillingKey, "사용자 요청에 의한 결제수단 변경");
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
            // 유예기간에 처음 들어가는 시점에만 알림 - 재시도 중 또 실패할 때마다 매번 보내진 않음
            // (사용자가 결제수단을 고칠 기회가 있다는 걸 알아야 하니, 접근이 끊기는 이 순간엔 반드시 안내)
            emailService.sendSubscriptionPastDue(payment.getUser().getUsername());
        }
    }

    // 유예기간을 다 썼거나(재시도 소진) 애초에 빌링키가 없을 때의 최종 취소 처리.
    // 구독 취소 + 접근 권한 해제 + 빌링키 로컬 정리까지 한 번에.
    //
    // notifyRenewalFailed: "결제 실패로 종료" 메일을 보낼지 여부는 호출부가 정한다.
    // 빌링키가 없어서 여기 오는 경우(attemptRenewalCharge)는 사실상 사용자가 직접 해지한 것뿐이라
    // (유예기간 중엔 빌링키를 안 지우고, PAST_DUE 구독은 cancel()로도 못 지움 -> 빌링키 소실 경로가
    //  자진 해지뿐임) 이미 cancel() 시점에 "해지 완료" 메일을 보냈음. 여기서 또 "결제 실패" 메일을
    // 보내면 자진 해지한 사람한테 사실과 다른 안내가 감.
    private void finalizeCancellation(Subscription subscription, User user, boolean notifyRenewalFailed) {
        subscription.cancel();
        user.setSubscribed(false);
        if (notifyRenewalFailed) {
            emailService.sendSubscriptionRenewalFailed(user.getUsername());
        }

        billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)
                .ifPresent(billingKey -> cleanUpBillingKey(billingKey, "구독 종료에 따른 빌링키 정리"));
    }

    @Transactional
    public void verifyAndFinalize(Payment payment) {
        // 웹훅과 completePayment/재결제 시도가 같은 paymentId를 거의 동시에 검증할 수 있어서
        // (check-then-act 레이스) 행 락을 걸어 재조회한다. 나중에 들어온 쪽은 먼저 것이 커밋될
        // 때까지 여기서 블록됐다가, 락이 풀리면 이미 PAID로 바뀐 상태를 보고 바로 아래 가드에서
        // 조용히 리턴된다. (문의 스레드 생성 동시성 제어와 동일한 패턴 재사용)
        payment = paymentRepository.findByPaymentIdForUpdate(payment.getPaymentId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 이미 PAID(완료)면 조용히 종료
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        // 포트원에 실제 결제 조회
        io.portone.sdk.server.payment.Payment portOnePayment;
        try {
            portOnePayment = portOneClient.getPayment().getPayment(payment.getPaymentId()).join();
        } catch (RuntimeException e) {
            // 재조회 자체가 실패한 경우 - 결제(payWithBillingKey)는 이미 끝났을 수도 있어서
            // 진짜 성공/실패인지 알 수가 없음. 그래서 확실치 않을 땐 일단 실패로 간주해 재시도 속도를
            // 매시간 -> 하루 1회로 늦추고, PortOne 콘솔에서 수동으로 확인할 시간을 벌어준다.
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("결제 검증 재조회 실패 - PortOne 콘솔에서 실제 청구 여부 수동 확인 필요 (paymentId={}, reason={})",
                    payment.getPaymentId(), cause.getMessage());
            markPaymentFailed(payment);
            // 여기서 조용히 return하면(예외를 안 던지면) completePayment()도 그냥 정상 종료돼서
            // 컨트롤러가 200("결제가 완료되었습니다")을 응답해버림 - 실제론 방금 FAILED로 처리했는데
            // 사용자에겐 성공했다고 알려주는 꼴. 그래서 다른 실패 분기들과 마찬가지로 예외를 던진다.
            // (attemptRenewalCharge/handleWebhook은 이미 이 메서드를 try/catch(BusinessException)로
            // 감싸서 로그만 남기고 넘어가므로, 여기서 던져도 그쪽 트랜잭션이 롤백되진 않는다.
            // completePayment()는 noRollbackFor=BusinessException.class라 지금까지 저장한
            // Payment/BillingKey 기록은 그대로 유지된 채 예외만 컨트롤러까지 전달된다.)
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

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
