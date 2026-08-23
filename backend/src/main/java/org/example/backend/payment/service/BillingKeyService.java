package org.example.backend.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.PortOneBillingKeyClient;
import org.example.backend.payment.dto.BillingKeyPrepareResponse;
import org.example.backend.payment.dto.BillingKeyResponse;
import org.example.backend.payment.dto.PortOneBillingKeyResponse;
import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.BillingKeyIssuanceIntent;
import org.example.backend.payment.entity.BillingKeyIssuanceIntentStatus;
import org.example.backend.payment.entity.BillingKeyStatus;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.BillingKeyIssuanceIntentRepository;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 빌링키 발급/선택/삭제 관련 서비스
@Service
@RequiredArgsConstructor
public class BillingKeyService {

    private final UserRepository userRepository;
    private final BillingKeyIssuanceIntentRepository billingKeyIssuanceIntentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PortOneBillingKeyClient portOneBillingKeyClient;
    private final PaymentService paymentService;

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
                .build();
    }

    // 빌링키 발급 검증: 발급 의도와 대조하고, 포트원 재조회로 확인 후 저장
    @Transactional
    public void verifyAndSaveBillingKey(String issueId, String billingKey, Long userId) {
        BillingKeyIssuanceIntent intent = billingKeyIssuanceIntentRepository.findByIssueId(issueId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_ISSUANCE_INTENT_NOT_FOUND));

        if (!intent.getUser().getId().equals(userId)) {
            throw new BusinessException(PaymentErrorCode.FORBIDDEN);
        }

        PortOneBillingKeyResponse portOneBillingKey = portOneBillingKeyClient.getBillingKey(billingKey);

        boolean channelMatched = false;
        for (PortOneBillingKeyResponse.Channel channel : portOneBillingKey.getChannels()) {
            if (billingChannelKey.equals(channel.getKey())) {
                channelMatched = true;
                break;
            }
        }
        boolean valid = "ISSUED".equals(portOneBillingKey.getStatus()) && channelMatched;
        if (!valid) {
            intent.setStatus(BillingKeyIssuanceIntentStatus.FAILED);
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        }

        // 이 유저의 ACTIVE 카드가 하나도 없으면(첫 카드) 자동 선택, 있으면 선택 안 함
        long activeCount = billingKeyRepository.countByUserIdAndStatus(userId, BillingKeyStatus.ACTIVE);
        boolean isFirstCard = activeCount == 0;

        BillingKey newBillingKey = new BillingKey();
        newBillingKey.setUser(intent.getUser());
        newBillingKey.setBillingKey(billingKey);
        newBillingKey.setStatus(BillingKeyStatus.ACTIVE);
        newBillingKey.setIsSelected(isFirstCard);
        billingKeyRepository.save(newBillingKey);

        intent.setStatus(BillingKeyIssuanceIntentStatus.ISSUED);
    }

    // 내 카드 목록 조회 (등록일 오름차순 -> 카드1, 카드2... 순번 매겨서 응답 DTO로 변환)
    @Transactional(readOnly = true)
    public List<BillingKeyResponse> getMyBillingKeys(Long userId) {
        List<BillingKey> billingKeys = billingKeyRepository.findByUserIdAndStatusOrderByCreatedAtAsc(userId, BillingKeyStatus.ACTIVE);

        List<BillingKeyResponse> responses = new ArrayList<>();
        for (int i = 0; i < billingKeys.size(); i++) {
            responses.add(BillingKeyResponse.from(billingKeys.get(i), i + 1));
        }
        return responses;
    }

    // 카드 선택 변경: 선택 상태를 옮기고, 예약된 자동결제가 있으면 새 카드로 재예약
    @Transactional
    public void selectBillingKey(Long userId, Long billingKeyId) {
        BillingKey target = billingKeyRepository.findById(billingKeyId)
                .filter(bk -> bk.getUser().getId().equals(userId) && bk.getStatus() == BillingKeyStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_TARGET_NOT_FOUND));

        if (Boolean.TRUE.equals(target.getIsSelected())) {
            return;
        }

        billingKeyRepository.findByUserIdAndStatusAndIsSelectedTrue(userId, BillingKeyStatus.ACTIVE)
                .ifPresent(current -> current.setIsSelected(false));

        target.setIsSelected(true);

        paymentService.reassignScheduleBillingKey(userId, target);
    }

    // 카드 삭제: 선택된 카드는 삭제 불가, 나머지는 소프트 딜리트
    @Transactional
    public void deleteBillingKey(Long userId, Long billingKeyId) {
        BillingKey target = billingKeyRepository.findById(billingKeyId)
                .filter(bk -> bk.getUser().getId().equals(userId) && bk.getStatus() == BillingKeyStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_TARGET_NOT_FOUND));

        if (Boolean.TRUE.equals(target.getIsSelected())) {
            throw new BusinessException(PaymentErrorCode.CANNOT_DELETE_SELECTED_BILLING_KEY);
        }

        target.setStatus(BillingKeyStatus.DELETED);
    }
}