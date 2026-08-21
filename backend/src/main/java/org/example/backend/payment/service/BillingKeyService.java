package org.example.backend.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.PortOneBillingKeyClient;
import org.example.backend.payment.dto.BillingKeyPrepareResponse;
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

import java.util.UUID;

// 빌링키 발급 관련 서비스
@Service
@RequiredArgsConstructor
public class BillingKeyService {

    private final UserRepository userRepository;
    private final BillingKeyIssuanceIntentRepository billingKeyIssuanceIntentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PortOneBillingKeyClient portOneBillingKeyClient;

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

    // 빌링키 발급 검증: 발급 의도와 대조하고, 포트원 재조회로 확인 후 저장
    @Transactional
    public void verifyAndSaveBillingKey(String issueId, String billingKey, Long userId) {
        // 발급 의도 찾음
        BillingKeyIssuanceIntent intent = billingKeyIssuanceIntentRepository.findByIssueId(issueId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_ISSUANCE_INTENT_NOT_FOUND));
        // 이 발급 의도가 진짜 지금 요청한 사람의 것인지 확인 로직
        if (!intent.getUser().getId().equals(userId)) {
            throw new BusinessException(PaymentErrorCode.FORBIDDEN);
        }

        // 프론트가 성공했다는것을 안믿고 백엔드에서 직접 포트원한테 재조회
        PortOneBillingKeyResponse portOneBillingKey = portOneBillingKeyClient.getBillingKey(billingKey);

        // 상태가 발급완료인지, 채널 목록중에 빌링채널키가 포함 되어있는지
        boolean channelMatched = false;
        for (PortOneBillingKeyResponse.Channel channel : portOneBillingKey.getChannels()) {
            if (billingChannelKey.equals(channel.getKey())) {
                channelMatched = true;
                break;
            }
        }
        boolean valid = "ISSUED".equals(portOneBillingKey.getStatus()) && channelMatched;
        // 검증 실패하면 발급의도 FAILED
        if (!valid) {
            intent.setStatus(BillingKeyIssuanceIntentStatus.FAILED);
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        }
        //검증 통과하면 저장
        BillingKey newBillingKey = new BillingKey();
        newBillingKey.setUser(intent.getUser());
        newBillingKey.setBillingKey(billingKey);
        newBillingKey.setStatus(BillingKeyStatus.ACTIVE);
        billingKeyRepository.save(newBillingKey);

        intent.setStatus(BillingKeyIssuanceIntentStatus.ISSUED);
    }
}