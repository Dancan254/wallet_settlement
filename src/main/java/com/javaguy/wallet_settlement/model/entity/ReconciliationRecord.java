package com.javaguy.wallet_settlement.model.entity;

import com.javaguy.wallet_settlement.model.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRecord {
    @Id
    @Column(name = "reconciliation_id")
    private String reconciliationId;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "internal_transaction_id")
    private String internalTransactionId;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "internal_amount", precision = 19, scale = 2)
    private BigDecimal internalAmount;

    @Column(name = "external_amount", precision = 19, scale = 2)
    private BigDecimal externalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationStatus status;

    @Column(name = "discrepancy_reason")
    private String discrepancyReason;

    @CreationTimestamp
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public BigDecimal getDiscrepancyAmount() {
        if (internalAmount == null) return externalAmount;
        if (externalAmount == null) return internalAmount.negate();
        return internalAmount.subtract(externalAmount);
    }
}

