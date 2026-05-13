package com.stockpro.purchase.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Same exchange as warehouse-service — all services share one exchange
    public static final String EXCHANGE = "stockpro.exchange";

    // Routing key for PO events
    public static final String PO_SUBMITTED_ROUTING_KEY = "po.submitted";
    public static final String PO_APPROVED_ROUTING_KEY  = "po.approved";
    public static final String PO_RECEIVED_ROUTING_KEY  = "po.received";

    // Queues
    public static final String PO_SUBMITTED_QUEUE = "po.submitted.queue";
    public static final String PO_APPROVED_QUEUE  = "po.approved.queue";
    public static final String PO_RECEIVED_QUEUE  = "po.received.queue";

    @Bean
    public TopicExchange stockproExchange() {
        return new TopicExchange(EXCHANGE);
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

    @Bean
    public Binding poSubmittedBinding(Queue poSubmittedQueue, TopicExchange stockproExchange) {
        return BindingBuilder.bind(poSubmittedQueue).to(stockproExchange).with(PO_SUBMITTED_ROUTING_KEY);
    }

    @Bean
    public Binding poApprovedBinding(Queue poApprovedQueue, TopicExchange stockproExchange) {
        return BindingBuilder.bind(poApprovedQueue).to(stockproExchange).with(PO_APPROVED_ROUTING_KEY);
    }

    @Bean
    public Binding poReceivedBinding(Queue poReceivedQueue, TopicExchange stockproExchange) {
        return BindingBuilder.bind(poReceivedQueue).to(stockproExchange).with(PO_RECEIVED_ROUTING_KEY);
    }

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
