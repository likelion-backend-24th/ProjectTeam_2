package org.example.backend.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.dto.BillingKeyPrepareResponse;
import org.example.backend.payment.entity.BillingKeyIssuanceIntent;
import org.example.backend.payment.entity.BillingKeyIssuanceIntentStatus;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.BillingKeyIssuanceIntentRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 빌링키 발급 관련 서비스
@Service
@RequiredArgsConstructor
public class BillingKeyService {

    private final UserRepository userRepository;
    private final BillingKeyIssuanceIntentRepository billingKeyIssuanceIntentRepository;

    @Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.billing-channel-key}")
    private String billingChannelKey;

    // 빌링키 발급 준비: 발급 의도를 먼저 기록하고, 프론트가 쓸 issueId를 발급함
    @Transactional
    public BillingKeyPrepareResponse prepareBillingKeyIssuance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.USER_NOT_FOUND));

        String issueId = "p2g-csh-billing-" + UUID.randomUUID().toString().replace("-", "");

        BillingKeyIssuanceIntent intent = new BillingKeyIssuanceIntent();
        intent.setUser(user);
        intent.setIssueId(issueId);
        intent.setStatus(BillingKeyIssuanceIntentStatus.READY);
        billingKeyIssuanceIntentRepository.save(intent);

        return BillingKeyPrepareResponse.builder()
                .storeId(storeId)
                .channelKey(billingChannelKey)
                .issueId(issueId)
                .build(); //이값을 프론트한테 돌려줌, 프론트는 이값을 받아서 포트원한테 상점id, 빌링 채널키,issueId로 카드 등록해달라함
    }
}