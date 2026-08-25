package org.example.backend.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.service.EmailService;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.common.util.AfterCommitExecutor;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public SubscriptionResponse subscribe(Long userId) {
        User user = loadSubscribableUser(userId);

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .user(user)
                .startedAt(now)
                .expiredAt(now.plusMonths(1))
                .build();
        subscriptionRepository.save(subscription);

        user.setSubscribed(true);
        AfterCommitExecutor.run(() -> emailService.sendSubscriptionStarted(user.getUsername()));
        return SubscriptionResponse.from(subscription);
    }

    /** 빌링키 기반 최초 구독. 이후 회차부터 자동 갱신된다. */
    @Transactional
    public void startWithAutoRenew(Long userId, LocalDateTime expiredAt) {
        User user = loadSubscribableUser(userId);

        Subscription subscription = Subscription.builder()
                .user(user)
                .startedAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .build();
        subscription.enableAutoRenew();
        subscriptionRepository.save(subscription);

        user.setSubscribed(true);
        AfterCommitExecutor.run(() -> emailService.sendSubscriptionStarted(user.getUsername()));
    }

    /**
     * 정기결제 성공(정상 갱신 또는 실패 후 재시도 성공) 시 이용 기간을 연장한다.
     * 연장 기준일은 구독 자신이 정한다 — 만료 전 갱신이면 남은 기간에 이어 붙는다.
     */
    @Transactional
    public void renewExisting(Long userId) {
        Subscription subscription = findUsableOrThrow(userId);
        subscription.renew(LocalDateTime.now());
        // 만료 시각을 넘겨 이용이 막혔던 구독이라면 결제 성공과 동시에 다시 열어준다.
        subscription.getUser().setSubscribed(true);
    }

    /** 정기결제 실패 시 호출. 재시도 한도를 넘기면 구독을 만료시킨다. */
    @Transactional
    public void recordPaymentFailure(Long userId) {
        Subscription subscription = findUsableOrThrow(userId);
        subscription.markPaymentFailed();
        if (subscription.hasExhaustedRetries()) {
            subscription.expire();
            subscription.getUser().setSubscribed(false);
        }
    }

    /** 다음 회차 자동 갱신을 멈춘다. 이용권 자체는 만료일까지 유지된다. */
    @Transactional
    public SubscriptionResponse cancel(Long userId) {
        Subscription subscription = findUsableOrThrow(userId);
        subscription.disableAutoRenew();
        notifyCancelled(userId);

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 카드 삭제처럼 "더 이상 청구하지 말라"는 요청에 딸려오는 해지.
     * 해지할 구독이 없거나 이미 해지 예약 상태면 아무 것도 하지 않는다 (카드만 지우면 되는 상황).
     */
    @Transactional
    public void disableAutoRenewIfUsable(Long userId) {
        subscriptionRepository.findFirstByUserIdAndStatusIn(userId, SubscriptionStatus.USABLE)
                .filter(Subscription::isAutoRenew)
                .ifPresent(subscription -> {
                    subscription.disableAutoRenew();
                    notifyCancelled(userId);
                });
    }

    private void notifyCancelled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.USER_NOT_FOUND));
        AfterCommitExecutor.run(() -> emailService.sendSubscriptionCancelled(user.getUsername()));
    }

    /** 만료 전 상태에서 자동 갱신을 다시 켠다. 카드 유효성은 실제 결제 시점에 확인된다. */
    @Transactional
    public SubscriptionResponse resume(Long userId) {
        Subscription subscription = findUsableOrThrow(userId);
        if (subscription.isAutoRenew()) {
            throw new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_AUTO_RENEW);
        }
        subscription.enableAutoRenew();
        return SubscriptionResponse.from(subscription);
    }

    public SubscriptionResponse getMy(Long userId) {
        return SubscriptionResponse.from(findUsableOrThrow(userId));
    }

    public boolean hasUsableSubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdAndStatusIn(userId, SubscriptionStatus.USABLE).isPresent();
    }

    // 관리자 구독 현황 조회 (상태 필터 + 유저 닉네임/이메일 검색). AdminService에서 사용
    public Page<Subscription> searchSubscriptionsForAdmin(String keyword, SubscriptionStatus status, Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
        return subscriptionRepository.searchSubscriptionsForAdmin(status, normalizedKeyword, pageable);
    }

    public boolean isPastDue(Long userId) {
        return subscriptionRepository.findFirstByUserIdAndStatusIn(userId, SubscriptionStatus.USABLE)
                .map(subscription -> subscription.getStatus() == SubscriptionStatus.PAST_DUE)
                .orElse(false);
    }

    private User loadSubscribableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.USER_INACTIVE);
        }

        subscriptionRepository.findFirstByUserIdAndStatusIn(userId, SubscriptionStatus.USABLE)
                .ifPresent(s -> { throw new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE); });

        return user;
    }

    private Subscription findUsableOrThrow(Long userId) {
        return subscriptionRepository.findFirstByUserIdAndStatusIn(userId, SubscriptionStatus.USABLE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }
}
