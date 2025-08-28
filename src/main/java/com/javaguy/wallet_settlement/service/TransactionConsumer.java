package com.javaguy.wallet_settlement.service;

import com.javaguy.wallet_settlement.model.dto.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.javaguy.wallet_settlement.config.RabbitMQConfig;

@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_QUEUE)
    public void handleTransactionEvent(TransactionEvent event) {
        try {
            logger.info("Processing transaction event: {} - {} - {}",
                    event.getTransactionId(), event.getType(), event.getAmount());

            logger.info("Successfully processed transaction event: {}", event.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to process transaction event: {}", event.getTransactionId(), e);
            throw e;
        }
    }
}
