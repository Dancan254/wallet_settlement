package com.javaguy.wallet_settlement.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String TRANSACTION_EXCHANGE = "transaction.exchange";
    public static final String TRANSACTION_QUEUE = "transaction.queue";
    public static final String TRANSACTION_ROUTING_KEY = "transaction.completed";
    public static final String TRANSACTION_DLQ = "transaction.dlq";
    public static final String TRANSACTION_DLX = "transaction.dlx";

    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(TRANSACTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue transactionQueue() {
        return QueueBuilder.durable(TRANSACTION_QUEUE)
                .withArgument("x-dead-letter-exchange", TRANSACTION_DLX)
                .withArgument("x-message-ttl", 300000) // -> 5 minutes
                .build();
    }

    @Bean
    public Queue transactionDLQ() {
        return QueueBuilder.durable(TRANSACTION_DLQ).build();
    }

    @Bean
    public DirectExchange transactionDLX() {
        return new DirectExchange(TRANSACTION_DLX);
    }

    @Bean
    public Binding transactionBinding() {
        return BindingBuilder.bind(transactionQueue())
                .to(transactionExchange())
                .with(TRANSACTION_ROUTING_KEY);
    }

    @Bean
    public Binding transactionDLQBinding() {
        return BindingBuilder.bind(transactionDLQ())
                .to(transactionDLX())
                .with("");
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}