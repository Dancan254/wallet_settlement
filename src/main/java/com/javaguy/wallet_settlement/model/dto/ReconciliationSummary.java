package com.javaguy.wallet_settlement.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReconciliationSummary {
    private int totalTransactions;
    private int matched;
    private int mismatched;
    private BigDecimal totalAmount;
    private BigDecimal matchedAmount;
    private BigDecimal discrepancyAmount;
}
