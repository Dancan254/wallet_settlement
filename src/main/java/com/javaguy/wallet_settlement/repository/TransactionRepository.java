package com.javaguy.wallet_settlement.repository;

import com.javaguy.wallet_settlement.model.entity.Transaction;
import com.javaguy.wallet_settlement.model.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);

    @Query("SELECT t FROM Transaction t WHERE DATE(t.createdAt) = :date")
    List<Transaction> findByCreatedAtDate(@Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t WHERE t.transactionId IN :transactionIds")
    List<Transaction> findByTransactionIdIn(@Param("transactionIds") List<String> transactionIds);

    @Query("SELECT t FROM Transaction t WHERE DATE(t.createdAt) = :date AND t.status = 'COMPLETED'")
    List<Transaction> findCompletedTransactionsByDate(@Param("date") LocalDate date);

    boolean existsByTransactionId(String transactionId);
}