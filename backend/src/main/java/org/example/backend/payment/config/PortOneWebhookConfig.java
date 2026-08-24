package org.example.backend.payment.config;

import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PortOneWebhookConfig {

    private final PortOneProperties portOneProperties;

    @Bean
    public WebhookVerifier webhookVerifier() {
        return new WebhookVerifier(portOneProperties.getWebhookSecret());
    }
}
