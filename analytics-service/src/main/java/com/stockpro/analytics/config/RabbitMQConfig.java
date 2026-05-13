package com.stockpro.analytics.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names — same as publisher side
    public static final String STOCK_MOVEMENT_QUEUE = "stock.movement.queue";
    public static final String PO_SUBMITTED_QUEUE   = "po.submitted.queue";
    public static final String PO_APPROVED_QUEUE    = "po.approved.queue";
    public static final String PO_RECEIVED_QUEUE    = "po.received.queue";

    // Exchange — shared across all services
    public static final String EXCHANGE = "stockpro.exchange";

    // Routing keys
    public static final String STOCK_MOVEMENT_KEY = "stock.movement";
    public static final String PO_SUBMITTED_KEY   = "po.submitted";
    public static final String PO_APPROVED_KEY    = "po.approved";
    public static final String PO_RECEIVED_KEY    = "po.received";

    // --- Exchange ---
    @Bean
    public TopicExchange stockproExchange() {
        return new TopicExchange(EXCHANGE);
    }

    // --- Queues (durable) ---
    @Bean
    public Queue stockMovementQueue() {
        return new Queue(STOCK_MOVEMENT_QUEUE, true);
    }

    @Bean
    public Queue poSubmittedQueue() {
        return new Queue(PO_SUBMITTED_QUEUE, true);
    }

    @Bean
    public Queue poApprovedQueue() {
        return new Queue(PO_APPROVED_QUEUE, true);
    }

    @Bean
    public Queue poReceivedQueue() {
        return new Queue(PO_RECEIVED_QUEUE, true);
    }

    // --- Bindings ---
    @Bean
    public Binding stockMovementBinding() {
        return BindingBuilder.bind(stockMovementQueue()).to(stockproExchange()).with(STOCK_MOVEMENT_KEY);
    }

    @Bean
    public Binding poSubmittedBinding() {
        return BindingBuilder.bind(poSubmittedQueue()).to(stockproExchange()).with(PO_SUBMITTED_KEY);
    }

    @Bean
    public Binding poApprovedBinding() {
        return BindingBuilder.bind(poApprovedQueue()).to(stockproExchange()).with(PO_APPROVED_KEY);
    }

    @Bean
    public Binding poReceivedBinding() {
        return BindingBuilder.bind(poReceivedQueue()).to(stockproExchange()).with(PO_RECEIVED_KEY);
    }

    // --- JSON Converter ---
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
