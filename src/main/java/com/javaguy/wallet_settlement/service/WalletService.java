package com.javaguy.wallet_settlement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaguy.wallet_settlement.exception.InsufficientFundsException;
import com.javaguy.wallet_settlement.exception.WalletAlreadyExistsException;
import com.javaguy.wallet_settlement.exception.WalletNotFoundException;
import com.javaguy.wallet_settlement.model.dto.*;
import com.javaguy.wallet_settlement.model.entity.TransactionLedger;
import com.javaguy.wallet_settlement.model.entity.Wallet;
import com.javaguy.wallet_settlement.model.enums.TransactionStatus;
import com.javaguy.wallet_settlement.model.enums.TransactionType;
import com.javaguy.wallet_settlement.repository.TransactionLedgerRepository;
import com.javaguy.wallet_settlement.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionLedgerRepository ledgerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public Wallet createWallet(String customerId) {
        if (walletRepository.existsByCustomerId(customerId)) {
            throw new WalletAlreadyExistsException("Wallet already exists for customer: " + customerId);
        }

        Wallet wallet = Wallet.builder()
                .walletId(UUID.randomUUID().toString())
                .customerId(customerId)
                .balance(BigDecimal.ZERO)
                .build();

        return walletRepository.save(wallet);
    }

    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    public WalletResponse topUp(String walletId, TopUpRequest request) {
        log.info("Processing top-up for wallet: {}, amount: {}", walletId, request.getAmount());

        // Check for duplicate transaction
        if (ledgerRepository.existsByExternalTransactionId(request.getTransactionId())) {
            log.warn("Duplicate transaction detected: {}", request.getTransactionId());
            return getExistingTransactionResponse(request.getTransactionId());
        }

        // Fetch wallet with optimistic lock
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        // Credit the wallet
        wallet.credit(request.getAmount());
        wallet = walletRepository.save(wallet);

        // Create ledger entry
        TransactionLedger ledger = createLedgerEntry(wallet, request.getAmount(),
                TransactionType.TOPUP, request.getTransactionId(),
                request.getDescription(), null, request.getMetadata());
        ledger.setStatus(TransactionStatus.COMPLETED);
        ledger.setProcessedAt(LocalDateTime.now());
        ledgerRepository.save(ledger);

        // Publish event
        publishTransactionEvent(ledger);

        log.info("Top-up completed for wallet: {}, new balance: {}", walletId, wallet.getBalance());

        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .customerId(wallet.getCustomerId())
                .transactionId(ledger.getTransactionId())
                .balanceAfter(wallet.getBalance())
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2))
    public WalletResponse consume(String walletId, ConsumeRequest request) {
        log.info("Processing consumption for wallet: {}, amount: {}, service: {}",
                walletId, request.getAmount(), request.getServiceType());

        // Check for duplicate transaction
        if (ledgerRepository.existsByExternalTransactionId(request.getTransactionId())) {
            log.warn("Duplicate transaction detected: {}", request.getTransactionId());
            return getExistingTransactionResponse(request.getTransactionId());
        }

        // Fetch wallet with optimistic lock
        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        // Validate sufficient funds
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Balance: %s, Required: %s",
                            wallet.getBalance(), request.getAmount()));
        }

        // Debit the wallet
        wallet.debit(request.getAmount());
        wallet = walletRepository.save(wallet);

        // Create ledger entry
        TransactionLedger ledger = createLedgerEntry(wallet, request.getAmount(),
                TransactionType.CONSUME, request.getTransactionId(),
                request.getDescription(), request.getServiceType(), request.getMetadata());
        ledger.setStatus(TransactionStatus.COMPLETED);
        ledger.setProcessedAt(LocalDateTime.now());
        ledgerRepository.save(ledger);

        // Publish event for async processing
        publishTransactionEvent(ledger);

        log.info("Consumption completed for wallet: {}, new balance: {}", walletId, wallet.getBalance());

        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .customerId(wallet.getCustomerId())
                .transactionId(ledger.getTransactionId())
                .balanceAfter(wallet.getBalance())
                .status("COMPLETED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        return BalanceResponse.builder()
                .walletId(wallet.getWalletId())
                .customerId(wallet.getCustomerId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .lastUpdated(wallet.getUpdatedAt())
                .build();
    }

    private TransactionLedger createLedgerEntry(Wallet wallet, BigDecimal amount,
                                                TransactionType type, String externalTransactionId, String description,
                                                String serviceType, Object metadata) {

        String metadataJson = null;
        if (metadata != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize metadata: {}", e.getMessage());
            }
        }

        return TransactionLedger.builder()
                .transactionId(UUID.randomUUID().toString())
                .walletId(wallet.getWalletId())
                .customerId(wallet.getCustomerId())
                .type(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .serviceType(serviceType)
                .externalTransactionId(externalTransactionId)
                .metadata(metadataJson)
                .status(TransactionStatus.PENDING)
                .build();
    }

    private WalletResponse getExistingTransactionResponse(String externalTransactionId) {
        TransactionLedger existingLedger = ledgerRepository.findByExternalTransactionId(externalTransactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction should exist but not found"));

        return WalletResponse.builder()
                .walletId(existingLedger.getWalletId())
                .customerId(existingLedger.getCustomerId())
                .transactionId(existingLedger.getTransactionId())
                .balanceAfter(existingLedger.getBalanceAfter())
                .status(existingLedger.getStatus().toString())
                .timestamp(existingLedger.getProcessedAt())
                .build();
    }

    private void publishTransactionEvent(TransactionLedger ledger) {
        // This will be handled by @TransactionalEventListener
        // to ensure the event is published only after successful commit
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(ledger.getTransactionId())
                .walletId(ledger.getWalletId())
                .customerId(ledger.getCustomerId())
                .type(ledger.getType())
                .amount(ledger.getAmount())
                .serviceType(ledger.getServiceType())
                .metadata(ledger.getMetadata())
                .build();

        eventPublisher.publishEvent(event);
    }
}
