package org.example.backend.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubscriptionResponse subscribe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(SubscriptionErrorCode.USER_INACTIVE);
        }

        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> { throw new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE); });

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .user(user)
                .startedAt(now)
                .expiredAt(now.plusMonths(1))
                .build();
        subscriptionRepository.save(subscription);

        user.setSubscribed(true);

        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse cancel(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        subscription.cancel();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.USER_NOT_FOUND));
        user.setSubscribed(false);

        return SubscriptionResponse.from(subscription);
    }

    public SubscriptionResponse getMy(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        return SubscriptionResponse.from(subscription);
    }
}