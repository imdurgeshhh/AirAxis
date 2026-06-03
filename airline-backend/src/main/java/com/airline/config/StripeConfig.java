package com.airline.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the Stripe SDK with the API key from application config.
 * This runs once at app startup — all subsequent Stripe calls use this key.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StripeConfig {

    private final AppProperties appProperties;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = appProperties.getPayment().getStripeSecretKey();
        log.info("Stripe SDK initialized (key prefix: {}...)",
                Stripe.apiKey.substring(0, Math.min(12, Stripe.apiKey.length())));
    }
}
