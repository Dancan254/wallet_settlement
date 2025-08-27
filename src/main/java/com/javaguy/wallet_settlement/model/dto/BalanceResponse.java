package com.javaguy.wallet_settlement.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BalanceResponse {
    private String walletId;
    private String customerId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime lastUpdated;
}
