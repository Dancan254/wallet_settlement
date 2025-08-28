package com.javaguy.wallet_settlement.model.dto;

import com.javaguy.wallet_settlement.model.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionEvent {
    private String transactionId;
    private Long walletId;
    private String type;
    private BigDecimal amount;
    private String description;
    private String status;
    private LocalDateTime timestamp;

    public TransactionEvent(Transaction transaction) {
        this.transactionId = transaction.getTransactionId();
        this.walletId = transaction.getWallet().getId();
        this.type = transaction.getType().name();
        this.amount = transaction.getAmount();
        this.description = transaction.getDescription();
        this.status = transaction.getStatus().name();
        this.timestamp = transaction.getCreatedAt();
    }
}
