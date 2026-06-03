package com.airline.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Type-safe binding for all custom 'airline.*' properties in application.yml.
 * Avoids @Value scatter — all app config in one place.
 */
@Data
@Component
@ConfigurationProperties(prefix = "airline")
public class AppProperties {

    private SeatLock seatLock = new SeatLock();
    private Booking booking = new Booking();
    private Payment payment = new Payment();
    private Cors cors = new Cors();

    @Data
    public static class SeatLock {
        private int ttlMinutes = 10;
    }

    @Data
    public static class Booking {
        private int pnrLength = 6;
    }

    @Data
    public static class Payment {
        private String stripeSecretKey;
        private String stripeWebhookSecret;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }
}
