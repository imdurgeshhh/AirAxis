package com.airline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing                 // Enables @CreatedDate, @LastModifiedDate on entities
@EnableAsync                       // Enables @Async for notification service
@EnableScheduling                  // Enables @Scheduled for seat-lock cleanup jobs
public class AirlineReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirlineReservationApplication.class, args);
    }
}
