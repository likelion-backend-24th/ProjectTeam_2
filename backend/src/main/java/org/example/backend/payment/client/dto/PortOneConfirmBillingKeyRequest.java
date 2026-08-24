package org.example.backend.payment.client.dto;

public record PortOneConfirmBillingKeyRequest(
        String storeId,
        String billingIssueToken,
        boolean isTest
) {
}
