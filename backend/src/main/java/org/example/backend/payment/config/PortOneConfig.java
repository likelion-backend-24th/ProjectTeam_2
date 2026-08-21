package org.example.backend.payment.config;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortOneConfig {
    @Value("${portone.api-secret}")
    private String apiSecret;

    @Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.webhook-secret}")
    private String webhookSecret;

    @Bean
    public PortOneClient portOneClient(){
        return new PortOneClient(apiSecret, "https://api.portone.io", storeId);
    }

    @Bean
    public WebhookVerifier webhookVerifier(){
        return new WebhookVerifier(webhookSecret);
    }
}
