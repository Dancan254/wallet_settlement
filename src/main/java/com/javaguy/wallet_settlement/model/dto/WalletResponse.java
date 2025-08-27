package com.javaguy.wallet_settlement.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletResponse {
    private String walletId;
    private String customerId;
    private String transactionId;
    private BigDecimal balanceAfter;
    private String status;
    private LocalDateTime timestamp;
}
