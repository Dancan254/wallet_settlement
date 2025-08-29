package com.javaguy.wallet_settlement.service;

import com.javaguy.wallet_settlement.exception.DuplicateTransactionException;
import com.javaguy.wallet_settlement.model.entity.Transaction;
import com.javaguy.wallet_settlement.model.entity.Wallet;
import com.javaguy.wallet_settlement.model.enums.TransactionStatus;
import com.javaguy.wallet_settlement.model.enums.TransactionType;
import com.javaguy.wallet_settlement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {


    private final TransactionRepository transactionRepository;
    private final TransactionPublisher transactionPublisher;

    @Transactional
    public Transaction createTransaction(String transactionId, Wallet wallet,
                                         TransactionType type,
                                         BigDecimal amount, String description,
                                         String requestId) {

        if (transactionRepository.existsByRequestId(requestId)) {
            // If a transaction with this requestId already exists, return it to ensure idempotency
            return transactionRepository.findByRequestId(requestId).orElseThrow(
                    () -> new DuplicateTransactionException("Duplicate requestId found but transaction not retrieved: " + requestId));
        }

        if (transactionRepository.existsByTransactionId(transactionId)) {
            throw new DuplicateTransactionException("Transaction already exists: " + transactionId);
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setRequestId(requestId);
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.COMPLETED);

        Transaction saved = transactionRepository.save(transaction);

        transactionPublisher.publishTransaction(saved);

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Transaction> findByRequestId(String requestId) {
        return transactionRepository.findByRequestId(requestId);
    }

    public List<Transaction> getTransactionsByDate(LocalDate date) {
        return transactionRepository.findByCreatedAtDate(date);
    }

    public List<Transaction> getTransactionsByIds(List<String> transactionIds) {
        return transactionRepository.findByTransactionIdIn(transactionIds);
    }

    public List<Transaction> getWalletTransactions(Wallet wallet) {
        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
    }
}
