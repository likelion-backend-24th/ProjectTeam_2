package org.example.backend.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.dto.PortOnePaymentResponse;
import org.example.backend.payment.config.PortOneProperties;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * PortOne V2 REST API 중 "결제 단건 조회"만 다룬다.
 * (io.portone:server-sdk 는 Kotlin suspend 기반이라 Java에서 다루기 번거로워
 *  일반결제 검증에 필요한 최소 범위는 REST API를 직접 호출한다.)
 */
@Slf4j
@Component
public class PortOnePaymentClient {

    private final RestClient restClient;
    private final String apiSecret;
    private final boolean testMode;

    public PortOnePaymentClient(PortOneProperties properties) {
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
     * test 쿼리 파라미터를 같이 보내면, 조회하려는 결제의 실제 테스트/실거래 여부가
     * 우리가 기대하는 것과 다를 때 PortOne이 400으로 거부해준다. (환경 오반영 방지)
     * @param paymentId 우리 서버가 발급한 paymentId
     */
    public PortOnePaymentResponse getPayment(String paymentId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}")
                            .queryParam("test", testMode)
                            .build(paymentId))
                    .header("Authorization", "PortOne " + apiSecret)
                    .retrieve()
                    .body(PortOnePaymentResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("PortOne 결제 단건 조회 실패 - 존재하지 않는 결제 건: paymentId={}", paymentId);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("PortOne 결제 환경(테스트/실연동) 불일치: paymentId={}", paymentId);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (Exception e) {
            log.error("PortOne API 통신 오류: paymentId={}", paymentId, e);
            throw new BusinessException(PaymentErrorCode.PORTONE_API_ERROR);
        }
    }
}