package com.stockpro.alert.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String EXCHANGE = "stockpro.exchange";
	public static final String LOW_STOCK_QUEUE = "alert.stock.low.queue";
	public static final String OVERSTOCK_QUEUE = "alert.stock.high.queue";
	public static final String PO_PENDING_QUEUE = "alert.po.pending.queue";
	public static final String PO_RECEIVED_QUEUE = "alert.po.received.queue"; // payment due
	public static final String STOCK_TRANSFER_REQUEST_QUEUE = "alert.stock.transfer.request.queue";
	public static final String STOCK_TRANSFER_REQUEST_COMPLETED_QUEUE = "alert.stock.transfer.request.completed.queue";
	public static final String STOCK_TRANSFER_QUEUE = "alert.stock.transfer.queue"; // transfer notify

	@Bean
	public TopicExchange stockExchange() {
		return new TopicExchange(EXCHANGE);
	}

	@Bean
	public Queue lowStockQueue() {
		return new Queue(LOW_STOCK_QUEUE, true);
	}

	@Bean
	public Queue overStockQueue() {
		return new Queue(OVERSTOCK_QUEUE, true);
	}

	@Bean
	public Queue poQueue() {
		return new Queue(PO_PENDING_QUEUE, true);
	}

	@Bean
	public Queue poReceivedQueue() {
		return new Queue(PO_RECEIVED_QUEUE, true);
	}

	@Bean
	public Queue stockTransferRequestQueue() {
		return new Queue(STOCK_TRANSFER_REQUEST_QUEUE, true);
	}

	@Bean
	public Queue stockTransferRequestCompletedQueue() {
		return new Queue(STOCK_TRANSFER_REQUEST_COMPLETED_QUEUE, true);
	}

	@Bean
	public Queue stockTransferQueue() {
		return new Queue(STOCK_TRANSFER_QUEUE, true);
	}

	@Bean
	public Binding lowBinding() {
		return BindingBuilder.bind(lowStockQueue()).to(stockExchange()).with("stock.low");
	}

	@Bean
	public Binding highBinding() { // NEW
		return BindingBuilder.bind(overStockQueue()).to(stockExchange()).with("stock.high");
	}

	@Bean
	public Binding poBinding() {
		return BindingBuilder.bind(poQueue()).to(stockExchange()).with("po.submitted");
	}

	@Bean
	public Binding poReceivedBinding() {
		return BindingBuilder.bind(poReceivedQueue()).to(stockExchange()).with("po.received");
	}

	@Bean
	public Binding stockTransferBinding() {
		return BindingBuilder.bind(stockTransferQueue()).to(stockExchange()).with("stock.transfer");
	}

	@Bean
	public Binding stockTransferRequestBinding() {
		return BindingBuilder.bind(stockTransferRequestQueue()).to(stockExchange()).with("stock.transfer.request");
	}

	@Bean
	public Binding stockTransferRequestCompletedBinding() {
		return BindingBuilder.bind(stockTransferRequestCompletedQueue()).to(stockExchange())
				.with("stock.transfer.request.completed");
	}

	// CRITICAL: JSON converter for deserializing incoming RabbitMQ messages
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

	// CRITICAL: Listener factory must use JSON converter for @RabbitListener to
	// work
	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(jsonMessageConverter());
		return factory;
	}
}
