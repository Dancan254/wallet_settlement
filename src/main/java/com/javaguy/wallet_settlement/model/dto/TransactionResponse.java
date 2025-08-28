package com.javaguy.wallet_settlement.model.dto;

import com.javaguy.wallet_settlement.model.enums.TransactionStatus;
import com.javaguy.wallet_settlement.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private String transactionId;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private TransactionStatus status;
}
