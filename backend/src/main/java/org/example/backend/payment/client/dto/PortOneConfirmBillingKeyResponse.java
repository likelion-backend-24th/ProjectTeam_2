package org.example.backend.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneConfirmBillingKeyResponse(String billingKey) {
}
