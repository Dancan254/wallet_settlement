package com.javaguy.wallet_settlement.model.entity;

import com.javaguy.wallet_settlement.model.enums.TransactionStatus;
import com.javaguy.wallet_settlement.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_ledger")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLedger {
    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "wallet_id", nullable = false)
    private String walletId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "external_transaction_id", unique = true)
    private String externalTransactionId;

    @Column(name = "service_type")
    private String serviceType;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}

