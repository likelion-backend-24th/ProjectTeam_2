package org.example.backend.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * PortOne 빌링키 단건 조회 응답 중 검증에 필요한 최소 필드만 매핑한다.
 * 채널은 단일 객체가 아니라 배열("channels")로 내려온다 — 카드 등록/청구에 사용된
 * 채널들의 목록이므로, 우리 채널 키가 그 안에 포함돼 있는지로 검증한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneBillingKeyResponse(
        String billingKey,
        String status,
        List<Channel> channels
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(String key) {
    }

    public boolean hasChannel(String channelKey) {
        return channels != null && channels.stream().anyMatch(c -> c.key() != null && c.key().equals(channelKey));
    }
}
