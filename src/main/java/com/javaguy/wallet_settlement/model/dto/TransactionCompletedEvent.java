package com.javaguy.wallet_settlement.model.dto;

import com.javaguy.wallet_settlement.model.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionCompletedEvent {
    private String transactionId;
    private String walletId;
    private String customerId;
    private TransactionType type;
    private BigDecimal amount;
    private String serviceType;
    private String metadata;
}
