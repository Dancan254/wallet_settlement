package com.javaguy.wallet_settlement.repository;

import com.javaguy.wallet_settlement.model.entity.TransactionLedger;
import com.javaguy.wallet_settlement.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionLedgerRepository extends JpaRepository<TransactionLedger, String> {

    boolean existsByExternalTransactionId(String externalTransactionId);

    Optional<TransactionLedger> findByExternalTransactionId(String externalTransactionId);

    Page<TransactionLedger> findByWalletIdOrderByCreatedAtDesc(String walletId, Pageable pageable);

    @Query("SELECT tl FROM TransactionLedger tl WHERE tl.walletId = :walletId AND tl.createdAt BETWEEN :startDate AND :endDate AND tl.status = :status")
    List<TransactionLedger> findTransactionsByDateRangeAndStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") TransactionStatus status);

    @Query("SELECT t FROM TransactionLedger t WHERE DATE(t.createdAt) = :date AND t.status = 'COMPLETED'")
    List<TransactionLedger> findCompletedTransactionsByDate(@Param("date") LocalDate date);
}