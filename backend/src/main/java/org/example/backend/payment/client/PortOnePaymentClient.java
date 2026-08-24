package org.example.backend.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.dto.PortOneBillingKeyPaymentRequest;
import org.example.backend.payment.client.dto.PortOneBillingKeyResponse;
import org.example.backend.payment.client.dto.PortOneConfirmBillingKeyRequest;
import org.example.backend.payment.client.dto.PortOneConfirmBillingKeyResponse;
import org.example.backend.payment.client.dto.PortOnePaymentResponse;
import org.example.backend.payment.config.PortOneProperties;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * PortOne V2 REST API 중 "결제/빌링키 단건 조회 및 청구"만 다룬다.
 * (io.portone:server-sdk 는 Kotlin suspend 기반이라 Java에서 다루기 번거로워
 *  검증에 필요한 최소 범위는 REST API를 직접 호출한다.)
 */
@Slf4j
@Component
public class PortOnePaymentClient {

    private static final String CURRENCY_KRW = "KRW";

    private final RestClient restClient;
    private final String storeId;
    private final String apiSecret;
    private final boolean testMode;

    public PortOnePaymentClient(PortOneProperties properties) {
        this.storeId = properties.getStoreId();
        this.apiSecret = properties.getApiSecret();
        this.testMode = properties.isTestMode();

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);

        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    /**
     * 결제 단건 조회.
     * storeId를 함께 보내지 않으면 이 계정에서는 결제 건을 찾지 못하고 404가 떨어진다.
     * (테스트/실거래 환경 검증은 응답의 channel.type으로 PaymentService에서 수행한다.
     *  test 쿼리 파라미터는 실제로 거부해주지 않아 신뢰할 수 없다.)
     * @param paymentId 우리 서버가 발급한 paymentId
     */
    public PortOnePaymentResponse getPayment(String paymentId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}")
                            .queryParam("storeId", storeId)
                            .build(paymentId))
                    .header("Authorization", "PortOne " + apiSecret)
                    .retrieve()
                    .body(PortOnePaymentResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("PortOne 결제 단건 조회 실패 - 존재하지 않는 결제 건: paymentId={}", paymentId);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (Exception e) {
            log.error("PortOne API 통신 오류: paymentId={}", paymentId, e);
            throw new BusinessException(PaymentErrorCode.PORTONE_API_ERROR);
        }
    }

    /**
     * 빌링키는 그 자체로 청구가 가능한 자격증명이다. DB에는 암호화해 저장하면서 로그에 원문을 남기면
     * 암호화가 무의미해지므로, 어디에도 원문을 남기지 않고 뒤 4자리만 남긴다.
     */
    private static String mask(String billingKey) {
        if (billingKey == null || billingKey.isBlank()) {
            return "(none)";
        }
        return billingKey.length() <= 4 ? "****" : "****" + billingKey.substring(billingKey.length() - 4);
    }

    /** PG 에러 본문이 빌링키를 그대로 되돌려주는 경우가 있어 본문에서도 가린다. */
    private static String maskIn(String responseBody, String billingKey) {
        if (responseBody == null) {
            return "";
        }
        if (billingKey == null || billingKey.isBlank()) {
            return responseBody;
        }
        return responseBody.replace(billingKey, mask(billingKey));
    }

    /**
     * 빌링키 단건 조회. 프론트가 전달한 빌링키를 그대로 믿지 않고 서버가 직접 확인한다.
     */
    public PortOneBillingKeyResponse getBillingKey(String billingKey) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/billing-keys/{billingKey}")
                            .queryParam("storeId", storeId)
                            .build(billingKey))
                    .header("Authorization", "PortOne " + apiSecret)
                    .retrieve()
                    .body(PortOneBillingKeyResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("PortOne 빌링키 조회 실패 - 존재하지 않는 빌링키. billingKey={}, body={}",
                    mask(billingKey), maskIn(e.getResponseBodyAsString(), billingKey));
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_NOT_FOUND);
        } catch (HttpClientErrorException e) {
            log.error("PortOne 빌링키 조회 실패. billingKey={}, status={}, body={}",
                    mask(billingKey), e.getStatusCode(), maskIn(e.getResponseBodyAsString(), billingKey));
            throw new BusinessException(PaymentErrorCode.PORTONE_API_ERROR);
        } catch (Exception e) {
            log.error("PortOne API 통신 오류(빌링키 조회)", e);
            throw new BusinessException(PaymentErrorCode.PORTONE_API_ERROR);
        }
    }

    /**
     * 채널이 "수동 승인"으로 설정된 경우, 카드 등록 직후 받는 빌링키는 실제 값이 아니라
     * 자리표시자(NEEDS_CONFIRMATION)다. 이 API로 발급을 확정해야 진짜 빌링키를 받을 수 있다.
     */
    public String confirmBillingKeyIssue(String billingIssueToken) {
        try {
            PortOneConfirmBillingKeyResponse response = restClient.post()
                    .uri("/billing-keys/confirm")
                    .header("Authorization", "PortOne " + apiSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PortOneConfirmBillingKeyRequest(storeId, billingIssueToken, testMode))
                    .retrieve()
                    .body(PortOneConfirmBillingKeyResponse.class);
            if (response == null || response.billingKey() == null) {
                throw new BusinessException(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
            }
            return response.billingKey();
        } catch (HttpClientErrorException e) {
            log.warn("빌링키 발급 승인 확정 실패. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        } catch (RestClientException e) {
            log.error("빌링키 발급 승인 확정 요청 실패", e);
            throw new BusinessException(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        }
    }

    /**
     * 빌링키를 PortOne에서 삭제한다. 우리 DB의 소프트 삭제와 별개로, PG에 남은 카드도 정리해야 한다.
     * 이미 삭제됐거나 존재하지 않는 키(404/409)는 우리가 원하는 상태와 같으므로 실패로 보지 않는다.
     */
    public void deleteBillingKey(String billingKey) {
        try {
            restClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/billing-keys/{billingKey}")
                            .queryParam("storeId", storeId)
                            .build(billingKey))
                    .header("Authorization", "PortOne " + apiSecret)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Conflict e) {
            log.info("PortOne 빌링키가 이미 삭제된 상태. billingKey={}, body={}",
                    mask(billingKey), maskIn(e.getResponseBodyAsString(), billingKey));
        } catch (HttpClientErrorException e) {
            log.error("PortOne 빌링키 삭제 실패. billingKey={}, status={}, body={}",
                    mask(billingKey), e.getStatusCode(), maskIn(e.getResponseBodyAsString(), billingKey));
            throw new BusinessException(PaymentErrorCode.PORTONE_API_ERROR);
        } catch (Exception e) {
            log.error("PortOne API 통신 오류(빌링키 삭제)", e);
            throw new BusinessException(PaymentErrorCode.PORTONE_API_ERROR);
        }
    }

    /**
     * 빌링키로 즉시 청구한다.
     * 청구 성공 여부는 이 응답이 아니라 이후 getPayment() 재조회 결과로 판단한다 (일반결제와 동일한 검증 경로 재사용).
     */
    public void payWithBillingKey(String paymentId, String billingKey, String customerId, String orderName, long amount) {
        PortOneBillingKeyPaymentRequest request = new PortOneBillingKeyPaymentRequest(
                storeId, billingKey, orderName, new PortOneBillingKeyPaymentRequest.Customer(customerId),
                new PortOneBillingKeyPaymentRequest.Amount(amount), CURRENCY_KRW);
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/payments/{paymentId}/billing-key").build(paymentId))
                    .header("Authorization", "PortOne " + apiSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            log.warn("빌링키 결제 실패. paymentId={}, status={}, body={}",
                    paymentId, e.getStatusCode(), maskIn(e.getResponseBodyAsString(), billingKey));
            throw new BusinessException(PaymentErrorCode.BILLING_PAYMENT_FAILED);
        } catch (RestClientException e) {
            log.error("빌링키 결제 요청 실패. paymentId={}", paymentId, e);
            throw new BusinessException(PaymentErrorCode.BILLING_PAYMENT_FAILED);
        }
    }
}
