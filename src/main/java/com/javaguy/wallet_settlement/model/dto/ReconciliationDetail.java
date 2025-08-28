package com.javaguy.wallet_settlement.model.dto;

import lombok.Builder;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Builder
public class ReconciliationDetail {
    private String transactionId;
    private String status;
    private String internalAmount;
    private String externalAmount;

    public ReconciliationDetail(String transactionId, String status, String internalAmount, String externalAmount) {
        this.transactionId = transactionId;
        this.status = status;
        this.internalAmount = internalAmount;
        this.externalAmount = externalAmount;
    }

}
