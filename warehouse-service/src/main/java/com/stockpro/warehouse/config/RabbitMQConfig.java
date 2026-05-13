package com.stockpro.warehouse.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange name - shared across all services
    public static final String EXCHANGE = "stockpro.exchange";

    // Routing key for stock movement events
    public static final String STOCK_MOVEMENT_ROUTING_KEY = "stock.movement";

    // 🔔 Alert routing keys — consumed by alert-service (PDF §2.7)
    public static final String STOCK_LOW_ROUTING_KEY      = "stock.low";
    public static final String STOCK_HIGH_ROUTING_KEY     = "stock.high";
    public static final String STOCK_TRANSFER_ROUTING_KEY = "stock.transfer";

    // Queue name - analytics-service will listen to this
    public static final String STOCK_MOVEMENT_QUEUE = "stock.movement.queue";

    @Bean
    public TopicExchange stockproExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue stockMovementQueue() {
        return new Queue(STOCK_MOVEMENT_QUEUE, true); // durable=true so queue survives restarts
    }

    @Bean
    public Binding stockMovementBinding(Queue stockMovementQueue, TopicExchange stockproExchange) {
        return BindingBuilder
                .bind(stockMovementQueue)
                .to(stockproExchange)
                .with(STOCK_MOVEMENT_ROUTING_KEY);
    }

    // JSON converter so messages are readable objects, not byte arrays
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
