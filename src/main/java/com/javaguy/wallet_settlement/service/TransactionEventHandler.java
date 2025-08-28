//package com.javaguy.wallet_settlement.service;
//
//import com.javaguy.wallet_settlement.config.RabbitMQConfig;
//import com.javaguy.wallet_settlement.model.dto.TransactionResponse;
//import com.javaguy.wallet_settlement.model.enums.TransactionType;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.event.TransactionPhase;
//import org.springframework.transaction.event.TransactionalEventListener;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class TransactionEventHandler {
//
//    private final RabbitTemplate rabbitTemplate;
//
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handleTransactionCompleted(TransactionResponse event) {
//        log.info("Publishing transaction completed event: {}", event.getTransactionId());
//
//        rabbitTemplate.convertAndSend(
//                RabbitMQConfig.TRANSACTION_EXCHANGE,
//                RabbitMQConfig.TRANSACTION_ROUTING_KEY,
//                event
//        );
//    }
//
//    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_QUEUE)
//    public void processTransactionEvent(TransactionResponse event) {
//        log.info("Processing transaction event: {} of type {} for service: {}",
//                event.getTransactionId(), event.getType(), event.getDescription());
//
//        // Only process service-specific logic for CONSUME transactions
//        if (event.getType() == TransactionType.CONSUME) {
//            try {
//                switch (event.getDescription()) {
//                    case "CRB_CHECK":
//                        processCRBCheck(event);
//                        break;
//                    case "KYC_VERIFY":
//                        processKYCVerification(event);
//                        break;
//                    case "CREDIT_SCORE":
//                        processCreditScore(event);
//                        break;
//                    default:
//                        log.info("No specific processing for service type: {}", event.getDescription());
//                }
//            } catch (Exception e) {
//                log.error("Error processing transaction event: {}", event.getTransactionId(), e);
//                throw e; // This will send the message to DLQ
//            }
//        } else if (event.getType() == TransactionType.TOPUP) {
//            log.info("No external service processing required for TOPUP transaction: {}", event.getTransactionId());
//        } else {
//            log.warn("Unhandled transaction type: {} for event: {}", event.getType(), event.getTransactionId());
//        }
//    }
//
//    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_DLQ)
//    public void handleFailedTransactionEvent(TransactionResponse event) {
//        log.error("Processing failed transaction from DLQ: {}", event.getTransactionId());
//        // Here you would implement manual intervention logic
//        // e.g., send alert to operations team, create support ticket, etc.
//    }
//
//    private void processCRBCheck(TransactionResponse event) {
//        // Mock external CRB service call
//        log.info("Processing CRB check for transaction: {}", event.getTransactionId());
//        // In real implementation, you would call external CRB API here
//        simulateExternalServiceCall(event.getDescription());
//    }
//
//    private void processKYCVerification(TransactionResponse event) {
//        // Mock external KYC service call
//        log.info("Processing KYC verification for transaction: {}", event.getTransactionId());
//        simulateExternalServiceCall(event.getDescription());
//    }
//
//    private void processCreditScore(TransactionResponse event) {
//        // Mock external credit scoring service call
//        log.info("Processing credit score for transaction: {}", event.getTransactionId());
//        simulateExternalServiceCall(event.getDescription());
//    }
//
//    private void simulateExternalServiceCall(String serviceType) {
//        // Simulate processing time
//        try {
//            Thread.sleep(100);
//            log.info("External service call completed for: {}", serviceType);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            log.warn("External service call interrupted for: {}", serviceType);
//        }
//    }
//}
