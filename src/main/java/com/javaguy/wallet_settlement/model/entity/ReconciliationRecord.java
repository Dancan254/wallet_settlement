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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "reconciliation_id", nullable = false, unique = true)
    private String reconciliationId;

    @Column(name = "internal_transaction_id")
    private String internalTransactionId;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "internal_amount", precision = 19, scale = 2)
    private BigDecimal internalAmount;

    @Column(name = "external_amount", precision = 19, scale = 2)
    private BigDecimal externalAmount;

    @Column(name = "discrepancy_amount", precision = 19, scale = 2)
    private BigDecimal discrepancyAmount;

    @Column(name = "discrepancy_reason")
    private String discrepancyReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

