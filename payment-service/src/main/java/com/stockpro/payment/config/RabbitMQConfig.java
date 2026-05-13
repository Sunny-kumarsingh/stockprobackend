package com.stockpro.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "stockpro.payments.exchange";
    public static final String STOCKPRO_EXCHANGE = "stockpro.exchange";
    public static final String PAYMENT_REQUESTED_QUEUE = "payment.requested.queue";
    public static final String PAYMENT_COMPLETED_QUEUE = "payment.completed.queue";
    public static final String PO_RECEIVED_PAYMENT_QUEUE = "payment.po.received.queue";
    public static final String ROUTING_REQUESTED = "payment.requested";
    public static final String ROUTING_COMPLETED = "payment.completed";
    public static final String PO_RECEIVED_ROUTING_KEY = "po.received";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange stockproExchange() {
        return new TopicExchange(STOCKPRO_EXCHANGE);
    }

    @Bean
    public Queue paymentRequestedQueue() {
        return new Queue(PAYMENT_REQUESTED_QUEUE, true);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return new Queue(PAYMENT_COMPLETED_QUEUE, true);
    }

    @Bean
    public Queue poReceivedPaymentQueue() {
        return new Queue(PO_RECEIVED_PAYMENT_QUEUE, true);
    }

    @Bean
    public Binding bindingRequested() {
        return BindingBuilder.bind(paymentRequestedQueue()).to(paymentExchange()).with(ROUTING_REQUESTED);
    }

    @Bean
    public Binding bindingCompleted() {
        return BindingBuilder.bind(paymentCompletedQueue()).to(paymentExchange()).with(ROUTING_COMPLETED);
    }

    @Bean
    public Binding bindingPoReceivedForPayment() {
        return BindingBuilder.bind(poReceivedPaymentQueue()).to(stockproExchange()).with(PO_RECEIVED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
