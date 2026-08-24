package org.example.backend.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * PortOne 빌링키 단건 조회 응답 중 검증에 필요한 최소 필드만 매핑한다.
 * 채널은 단일 객체가 아니라 배열("channels")로 내려온다 — 카드 등록/청구에 사용된
 * 채널들의 목록이므로, 우리 채널 키가 그 안에 포함돼 있는지로 검증한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneBillingKeyResponse(
        String billingKey,
        String status,
        List<Channel> channels,
        List<Method> methods
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(String key) {
    }

    /** 등록된 결제수단. 카드 등록만 지원하므로 사실상 카드 한 건이다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Method(Card card) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Card(String name, String number) {
        }
    }

    /** 카드전표인자명(예: 신한카드). 카드사·채널에 따라 내려오지 않을 수 있어 null을 허용한다. */
    public String cardName() {
        return firstCard().map(Method.Card::name).orElse(null);
    }

    /** 마스킹된 카드번호. 마찬가지로 항상 오지는 않는다. */
    public String maskedCardNumber() {
        return firstCard().map(Method.Card::number).orElse(null);
    }

    private Optional<Method.Card> firstCard() {
        if (methods == null) {
            return Optional.empty();
        }
        return methods.stream().map(Method::card).filter(Objects::nonNull).findFirst();
    }

    public boolean hasChannel(String channelKey) {
        return channels != null && channels.stream().anyMatch(c -> c.key() != null && c.key().equals(channelKey));
    }
}
