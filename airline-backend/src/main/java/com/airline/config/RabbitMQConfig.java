package com.airline.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ---- Exchange ----
    public static final String AIRLINE_EXCHANGE = "airline.events";

    // ---- Queues ----
    public static final String BOOKING_CONFIRMED_QUEUE   = "booking.confirmed";
    public static final String BOOKING_CANCELLED_QUEUE   = "booking.cancelled";
    public static final String PAYMENT_SUCCESS_QUEUE     = "payment.success";
    public static final String PAYMENT_FAILED_QUEUE      = "payment.failed";

    // ---- Routing Keys ----
    public static final String BOOKING_CONFIRMED_KEY     = "booking.confirmed";
    public static final String BOOKING_CANCELLED_KEY     = "booking.cancelled";
    public static final String PAYMENT_SUCCESS_KEY       = "payment.success";
    public static final String PAYMENT_FAILED_KEY        = "payment.failed";

    @Bean
    public TopicExchange airlineExchange() {
        return new TopicExchange(AIRLINE_EXCHANGE, true, false);
    }

    @Bean
    public Queue bookingConfirmedQueue() {
        return QueueBuilder.durable(BOOKING_CONFIRMED_QUEUE).build();
    }

    @Bean
    public Queue bookingCancelledQueue() {
        return QueueBuilder.durable(BOOKING_CANCELLED_QUEUE).build();
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }

    @Bean
    public Binding bookingConfirmedBinding() {
        return BindingBuilder.bind(bookingConfirmedQueue())
                .to(airlineExchange()).with(BOOKING_CONFIRMED_KEY);
    }

    @Bean
    public Binding bookingCancelledBinding() {
        return BindingBuilder.bind(bookingCancelledQueue())
                .to(airlineExchange()).with(BOOKING_CANCELLED_KEY);
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(airlineExchange()).with(PAYMENT_SUCCESS_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(airlineExchange()).with(PAYMENT_FAILED_KEY);
    }

    // Serialize messages as JSON (not Java serialization)
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
