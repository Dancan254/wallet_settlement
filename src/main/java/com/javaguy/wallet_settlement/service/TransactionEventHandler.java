package com.javaguy.wallet_settlement.service;

import com.javaguy.wallet_settlement.config.RabbitMQConfig;
import com.javaguy.wallet_settlement.model.dto.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Publishing transaction completed event: {}", event.getTransactionId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRANSACTION_EXCHANGE,
                RabbitMQConfig.TRANSACTION_ROUTING_KEY,
                event
        );
    }

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_QUEUE)
    public void processTransactionEvent(TransactionCompletedEvent event) {
        log.info("Processing transaction event: {} for service: {}",
                event.getTransactionId(), event.getServiceType());

        try {
            // Here you would call external services based on the service type
            switch (event.getServiceType()) {
                case "CRB_CHECK":
                    processCRBCheck(event);
                    break;
                case "KYC_VERIFY":
                    processKYCVerification(event);
                    break;
                case "CREDIT_SCORE":
                    processCreditScore(event);
                    break;
                default:
                    log.info("No specific processing for service type: {}", event.getServiceType());
            }
        } catch (Exception e) {
            log.error("Error processing transaction event: {}", event.getTransactionId(), e);
            throw e; // This will send the message to DLQ
        }
    }

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_DLQ)
    public void handleFailedTransactionEvent(TransactionCompletedEvent event) {
        log.error("Processing failed transaction from DLQ: {}", event.getTransactionId());
        // Here you would implement manual intervention logic
        // e.g., send alert to operations team, create support ticket, etc.
    }

    private void processCRBCheck(TransactionCompletedEvent event) {
        // Mock external CRB service call
        log.info("Processing CRB check for transaction: {}", event.getTransactionId());
        // In real implementation, you would call external CRB API here
        simulateExternalServiceCall(event.getServiceType());
    }

    private void processKYCVerification(TransactionCompletedEvent event) {
        // Mock external KYC service call
        log.info("Processing KYC verification for transaction: {}", event.getTransactionId());
        simulateExternalServiceCall(event.getServiceType());
    }

    private void processCreditScore(TransactionCompletedEvent event) {
        // Mock external credit scoring service call
        log.info("Processing credit score for transaction: {}", event.getTransactionId());
        simulateExternalServiceCall(event.getServiceType());
    }

    private void simulateExternalServiceCall(String serviceType) {
        // Simulate processing time
        try {
            Thread.sleep(100);
            log.info("External service call completed for: {}", serviceType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("External service call interrupted for: {}", serviceType);
        }
    }
}
