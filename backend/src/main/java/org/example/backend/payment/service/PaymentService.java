package org.example.backend.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.PortOnePaymentClient;
import org.example.backend.payment.client.dto.PortOneBillingKeyResponse;
import org.example.backend.payment.client.dto.PortOnePaymentResponse;
import org.example.backend.payment.config.PortOneProperties;
import org.example.backend.payment.dto.response.BillingKeyPrepareResponse;
import org.example.backend.payment.dto.response.BillingKeyResponse;
import org.example.backend.payment.dto.response.PaymentReadyResponse;
import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.Payment;
import org.example.backend.payment.entity.PaymentPurpose;
import org.example.backend.payment.entity.PaymentStatus;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.service.SubscriptionService;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final String BILLING_KEY_ISSUED_STATUS = "ISSUED";

    /** 수동 재시도 최소 간격. 실패해도 횟수 제한에 걸리지 않으므로 연타만 막는다. */
    private static final long MANUAL_RETRY_COOLDOWN_SECONDS = 60;

    private final PaymentRepository paymentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final PortOnePaymentClient portOnePaymentClient;
    private final PortOneProperties portOneProperties;

    @Transactional
    public PaymentReadyResponse readySubscriptionPayment(Long userId) {
        User user = loadActiveUser(userId, PaymentErrorCode.USER_NOT_FOUND, PaymentErrorCode.USER_INACTIVE);

        if (subscriptionService.hasUsableSubscription(userId)) {
            throw new BusinessException(PaymentErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        String paymentId = portOneProperties.getPaymentIdPrefix() + UUID.randomUUID();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .user(user)
                .purpose(PaymentPurpose.SUBSCRIPTION)
                .amount(portOneProperties.getSubscription().getAmount())
                .currency(portOneProperties.getSubscription().getCurrency())
                .orderName(portOneProperties.getSubscription().getOrderName())
                .build();
        paymentRepository.save(payment);

        return PaymentReadyResponse.of(payment, portOneProperties.getStoreId(), portOneProperties.getChannelKeyPayment());
    }

    @Transactional
    public SubscriptionResponse completeSubscriptionPayment(Long userId, String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.belongsTo(userId)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_FORBIDDEN);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            // 이미 성공 처리된 결제에 완료 API가 다시 호출된 경우(새로고침, 네트워크 재시도 등)
            // 에러 대신 현재 구독 상태를 그대로 돌려준다. (가이드 5.3 완료 API 계약 - 멱등성)
            log.info("이미 처리된 결제에 대한 완료 요청 - 현재 상태 반환: paymentId={}", paymentId);
            return subscriptionService.getMy(userId);
        }

        if (!payment.isReady()) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        PortOnePaymentResponse portOnePayment = portOnePaymentClient.getPayment(paymentId);
        String failReason = verify(payment, portOnePayment, portOneProperties.getChannelKeyPayment());

        if (failReason != null) {
            payment.markFailed(failReason);
            log.warn("결제 검증 실패: paymentId={}, reason={}", paymentId, failReason);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        LocalDateTime paidAt = portOnePayment.paidAt() != null
                ? LocalDateTime.ofInstant(portOnePayment.paidAt(), ZoneId.systemDefault())
                : LocalDateTime.now();
        payment.markPaid(paidAt);

        return subscriptionService.subscribe(userId);
    }

    /**
     * 카드(빌링키) 등록 준비. PortOne SDK의 카드 등록 팝업 호출에 필요한 값만 내려준다.
     * 실제 검증은 카드 등록 후 completeBillingKeyIssue()에서 PortOne 재조회로 이뤄진다.
     */
    public BillingKeyPrepareResponse prepareBillingKeyIssue(Long userId) {
        User user = loadActiveUser(userId, PaymentErrorCode.USER_NOT_FOUND, PaymentErrorCode.USER_INACTIVE);

        return BillingKeyPrepareResponse.builder()
                .storeId(portOneProperties.getStoreId())
                .channelKey(portOneProperties.getChannelKeyBilling())
                .issueId(UUID.randomUUID().toString())
                .customerId(customerIdOf(user))
                .build();
    }

    /**
     * 프론트가 PortOne SDK로 발급받은 빌링키를 그대로 믿지 않고 서버가 검증한 뒤 저장한다.
     * 채널이 수동 승인으로 설정된 경우 billingKey는 "NEEDS_CONFIRMATION" 자리표시자로 오고
     * billingIssueToken으로 승인을 확정해야 진짜 빌링키를 받을 수 있다 — 이 경우엔 그 확정 호출
     * 자체가 우리 서버(api-secret)로 인증된 것이라 신뢰 근거가 되므로 재조회를 따로 하지 않는다.
     * 즉시발급인 경우엔 기존처럼 재조회로 상태/채널을 검증한다.
     * 기존 활성 카드가 있으면 소프트 삭제하고 새 카드로 교체한다 (유저당 활성 카드 1개).
     */
    @Transactional
    public void completeBillingKeyIssue(Long userId, String billingKey, String billingIssueToken) {
        User user = loadActiveUser(userId, PaymentErrorCode.USER_NOT_FOUND, PaymentErrorCode.USER_INACTIVE);

        String verifiedBillingKey;
        PortOneBillingKeyResponse issued;
        if (billingIssueToken != null && !billingIssueToken.isBlank()) {
            verifiedBillingKey = portOnePaymentClient.confirmBillingKeyIssue(billingIssueToken);
            issued = findBillingKeyForDisplay(verifiedBillingKey);
        } else {
            issued = portOnePaymentClient.getBillingKey(billingKey);
            verifyIssuedBillingKey(issued);
            verifiedBillingKey = billingKey;
        }

        billingKeyRepository.findByUserIdAndDeletedAtIsNull(userId).ifPresent(BillingKey::delete);

        BillingKey entity = BillingKey.builder()
                .user(user)
                .billingKeyToken(verifiedBillingKey)
                .cardName(issued == null ? null : issued.cardName())
                .cardNumberMasked(issued == null ? null : issued.maskedCardNumber())
                .issuedAt(LocalDateTime.now())
                .build();
        billingKeyRepository.save(entity);
    }

    /**
     * 마이페이지에 보여줄 카드 정보를 읽어온다. 발급 확정 경로에서는 이미 서버 인증으로 신뢰가 확보돼 있어
     * 검증 목적이 아니라 표시 목적의 조회다. 실패해도 카드 등록 자체를 막지는 않는다.
     */
    private PortOneBillingKeyResponse findBillingKeyForDisplay(String billingKey) {
        try {
            return portOnePaymentClient.getBillingKey(billingKey);
        } catch (BusinessException e) {
            log.warn("빌링키 카드 정보 조회 실패 - 카드 표시 정보 없이 등록한다.", e);
            return null;
        }
    }

    /** 등록된 카드 조회. 카드번호·카드사는 보관하지 않으므로 등록 여부와 등록 시각만 알려준다. */
    public BillingKeyResponse getMyBillingKey(Long userId) {
        return billingKeyRepository.findByUserIdAndDeletedAtIsNull(userId)
                .map(BillingKeyResponse::of)
                .orElseGet(BillingKeyResponse::empty);
    }

    /**
     * 등록된 카드를 삭제한다. PortOne에서도 함께 지우고, 이용 중인 구독이 있으면 자동 갱신을 끈다 —
     * 카드를 지운 사용자는 다음 회차를 결제할 의사가 없다고 보는 게 맞고,
     * 청구할 카드 없이 자동 갱신만 켜져 있으면 결제 실패만 쌓인다.
     * 이미 결제된 이용 기간은 만료일까지 그대로 유지된다(환불 없음).
     */
    @Transactional
    public void deleteBillingKey(Long userId) {
        BillingKey billingKey = billingKeyRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_NOT_FOUND));

        try {
            portOnePaymentClient.deleteBillingKey(billingKey.getBillingKeyToken());
        } catch (BusinessException e) {
            // PG 삭제가 실패해도 우리 기록은 지운다. 우리가 보관한 빌링키가 없으면 청구 자체가 불가능하므로
            // "더 이상 청구하지 말라"는 요청은 지켜진다. PortOne에 남은 키는 콘솔에서 정리해야 한다.
            log.error("PortOne 빌링키 삭제 실패 - 로컬 기록만 삭제한다. userId={}", userId, e);
        }

        billingKey.delete();
        subscriptionService.disableAutoRenewIfUsable(userId);
    }

    private void verifyIssuedBillingKey(PortOneBillingKeyResponse issued) {
        boolean channelMatches = issued.hasChannel(portOneProperties.getChannelKeyBilling());
        if (!BILLING_KEY_ISSUED_STATUS.equalsIgnoreCase(issued.status()) || !channelMatches) {
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        }
    }

    /** 카드 등록 + 즉시 청구로 정기결제 구독을 시작한다. */
    @Transactional
    public SubscriptionResponse subscribeWithBillingKey(Long userId) {
        User user = loadActiveUser(userId, PaymentErrorCode.USER_NOT_FOUND, PaymentErrorCode.USER_INACTIVE);

        if (subscriptionService.hasUsableSubscription(userId)) {
            throw new BusinessException(PaymentErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        if (!chargeSubscription(user, SubscriptionChargeMode.INITIAL)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
        return subscriptionService.getMy(userId);
    }

    /**
     * PAST_DUE 상태에서 사용자가 수동으로 다시 결제를 시도한다. 실패해도 예외 없이 현재 상태를 반환한다.
     * 실패를 재시도 횟수로 세지 않으므로(MANUAL_RETRY) 스케줄러의 자동 재시도 기회는 그대로 남는다.
     * 대신 횟수 제한이 없어지는 만큼, 연타로 PG를 반복 호출하지 않도록 직전 청구와의 간격만 확인한다.
     */
    @Transactional
    public SubscriptionResponse retrySubscriptionPayment(Long userId) {
        if (!subscriptionService.isPastDue(userId)) {
            throw new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_PAST_DUE);
        }
        if (paymentRepository.existsByUserIdAndBillingKeyIsNotNullAndCreatedAtAfter(
                userId, LocalDateTime.now().minusSeconds(MANUAL_RETRY_COOLDOWN_SECONDS))) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_RETRY_TOO_SOON);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.USER_NOT_FOUND));

        chargeSubscription(user, SubscriptionChargeMode.MANUAL_RETRY);
        return subscriptionService.getMy(userId);
    }

    /**
     * 해지 예약 상태에서 자동 갱신을 다시 켠다.
     * 등록된 카드가 없으면 켜지 못하게 막는다 — 카드 없이 자동 갱신만 켜면 스케줄러가 매일 청구에 실패하고,
     * 3회 만에 아직 남아 있던 이용 기간까지 만료시켜 버린다.
     */
    @Transactional
    public SubscriptionResponse resumeAutoRenew(Long userId) {
        if (billingKeyRepository.findByUserIdAndDeletedAtIsNull(userId).isEmpty()) {
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_NOT_FOUND);
        }
        return subscriptionService.resume(userId);
    }

    /**
     * 빌링키로 청구를 시도한다. 최초 구독/수동 재시도/정기 스케줄러 재시도가 모두 이 메서드를 공유하고,
     * 성공·실패 후처리만 mode에 따라 갈린다.
     * 카드사 거절 등 "정상적으로 실패할 수 있는" 케이스는 예외를 던지지 않고 boolean으로만 알린다 —
     * 예외를 던지면 트랜잭션이 롤백되어 방금 늘어난 실패 횟수(retryCount)까지 함께 사라지기 때문이다.
     *
     * @return 청구 성공 여부
     */
    @Transactional
    public boolean chargeSubscription(User user, SubscriptionChargeMode mode) {
        BillingKey billingKey = billingKeyRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_NOT_FOUND));

        String paymentId = portOneProperties.getPaymentIdPrefix() + UUID.randomUUID();
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .user(user)
                .purpose(PaymentPurpose.SUBSCRIPTION)
                .amount(portOneProperties.getSubscription().getAmount())
                .currency(portOneProperties.getSubscription().getCurrency())
                .orderName(portOneProperties.getSubscription().getOrderName())
                .billingKey(billingKey)
                .build();
        paymentRepository.save(payment);

        String failReason = chargeAndVerify(payment, billingKey);
        if (failReason != null) {
            payment.markFailed(failReason);
            log.warn("정기결제 청구 실패: paymentId={}, userId={}, mode={}, reason={}",
                    paymentId, user.getId(), mode, failReason);
            if (mode.countsFailure()) {
                subscriptionService.recordPaymentFailure(user.getId());
            }
            return false;
        }

        LocalDateTime paidAt = LocalDateTime.now();
        payment.markPaid(paidAt);
        billingKey.markUsed(paidAt);
        if (mode.isRenewal()) {
            // 갱신 기준일은 구독이 정한다 — 만료 전에 미리 청구된 경우 남은 기간에 이어 붙어야 한다.
            subscriptionService.renewExisting(user.getId());
        } else {
            subscriptionService.startWithAutoRenew(user.getId(), LocalDateTime.now().plusMonths(1));
        }
        return true;
    }

    /**
     * @return 실패 사유. 검증을 통과하면 null.
     */
    private String chargeAndVerify(Payment payment, BillingKey billingKey) {
        try {
            portOnePaymentClient.payWithBillingKey(payment.getPaymentId(), billingKey.getBillingKeyToken(),
                    customerIdOf(payment.getUser()), payment.getOrderName(), payment.getAmount());
        } catch (BusinessException e) {
            return "빌링키 결제 요청 실패: " + e.getMessage();
        }

        PortOnePaymentResponse portOnePayment = portOnePaymentClient.getPayment(payment.getPaymentId());
        return verify(payment, portOnePayment, portOneProperties.getChannelKeyBilling());
    }

    /**
     * @return 검증 실패 사유. 검증을 통과하면 null.
     */
    private String verify(Payment payment, PortOnePaymentResponse portOnePayment, String expectedChannelKey) {
        if (!"PAID".equalsIgnoreCase(portOnePayment.status())) {
            return "결제 상태가 PAID가 아님: " + portOnePayment.status();
        }
        if (!Objects.equals(portOneProperties.getStoreId(), portOnePayment.storeId())) {
            return "storeId 불일치";
        }
        if (portOnePayment.channel() == null || !Objects.equals(expectedChannelKey, portOnePayment.channel().key())) {
            return "channelKey 불일치";
        }
        // 테스트 채널로 설정해뒀는데 실거래가 잡히는(혹은 그 반대) 환경 오반영을 막는다.
        String expectedChannelType = portOneProperties.isTestMode() ? "TEST" : "LIVE";
        if (!expectedChannelType.equalsIgnoreCase(portOnePayment.channel().type())) {
            return "결제 환경(테스트/실연동) 불일치: " + portOnePayment.channel().type();
        }
        if (portOnePayment.amount() == null || !Objects.equals(payment.getAmount(), portOnePayment.amount().total())) {
            return "결제 금액 불일치";
        }
        if (!Objects.equals(payment.getCurrency(), portOnePayment.currency())) {
            return "통화 불일치";
        }
        return null;
    }

    private User loadActiveUser(Long userId, PaymentErrorCode notFound, PaymentErrorCode inactive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(notFound));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(inactive);
        }
        return user;
    }

    /**
     * PortOne customerKey는 2자 이상이어야 한다 — 한 자리 유저 ID를 그대로 쓰면 검증에 걸린다.
     */
    private String customerIdOf(User user) {
        return "user-" + user.getId();
    }
}
