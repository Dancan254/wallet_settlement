package com.javaguy.wallet_settlement.service;

import com.javaguy.wallet_settlement.config.RabbitMQConfig;
import com.javaguy.wallet_settlement.model.dto.TransactionEvent;
import com.javaguy.wallet_settlement.model.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishTransaction(Transaction transaction) {
        try {
            TransactionEvent event = new TransactionEvent(transaction);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TRANSACTION_EXCHANGE,
                    RabbitMQConfig.TRANSACTION_ROUTING_KEY,
                    event
            );
            logger.info("Published transaction event: {}", transaction.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to publish transaction event: {}", transaction.getTransactionId(), e);
        }
    }
}
