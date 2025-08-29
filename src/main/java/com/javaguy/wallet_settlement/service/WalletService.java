package com.javaguy.wallet_settlement.service;

import com.javaguy.wallet_settlement.exception.InsufficientFundsException;
import com.javaguy.wallet_settlement.exception.WalletAlreadyExistsException;
import com.javaguy.wallet_settlement.exception.WalletNotFoundException;
import com.javaguy.wallet_settlement.model.dto.ConsumeRequest;
import com.javaguy.wallet_settlement.model.dto.CreateWalletRequest;
import com.javaguy.wallet_settlement.model.dto.TopUpRequest;
import com.javaguy.wallet_settlement.model.dto.TransactionResponse;
import com.javaguy.wallet_settlement.model.dto.WalletResponse;
import com.javaguy.wallet_settlement.model.entity.Transaction;
import com.javaguy.wallet_settlement.model.entity.Wallet;
import com.javaguy.wallet_settlement.model.enums.TransactionType;
import com.javaguy.wallet_settlement.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        String customerId = request.getCustomerId();
        if (walletRepository.findByCustomerId(customerId).isPresent()) {
            throw new WalletAlreadyExistsException("Wallet already exists for customer: " + customerId);
        }

        Wallet newWallet = new Wallet();
        newWallet.setCustomerId(customerId);
        newWallet.setBalance(BigDecimal.ZERO);
        Wallet savedWallet = walletRepository.save(newWallet);

        return new WalletResponse(savedWallet.getCustomerId(), savedWallet.getBalance());
    }

    @Transactional
    public TransactionResponse topUp(String customerId, TopUpRequest request) {
        // Check for existing transaction with the same requestId to ensure idempotency
        Optional<Transaction> existingTransaction = transactionService.findByRequestId(request.getRequestId());
        if (existingTransaction.isPresent()) {
            return toTransactionResponse(existingTransaction.get());
        }

        Wallet wallet = getOrCreateWallet(customerId);
        String transactionId = generateTransactionId();

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        Transaction transaction = transactionService.createTransaction(
                transactionId,
                wallet,
                TransactionType.TOPUP,
                request.getAmount(),
                request.getDescription(),
                request.getRequestId()
        );

        return toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse consume(String customerId, ConsumeRequest request) {
        // Check for existing transaction with the same requestId to ensure idempotency
        Optional<Transaction> existingTransaction = transactionService.findByRequestId(request.getRequestId());
        if (existingTransaction.isPresent()) {
            return toTransactionResponse(existingTransaction.get());
        }

        Wallet wallet = walletRepository.findByCustomerIdWithLock(customerId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for customer: " + customerId));

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance. Available: " + wallet.getBalance() + ", Required: " + request.getAmount());
        }

        String transactionId = generateTransactionId();

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        Transaction transaction = transactionService.createTransaction(
                transactionId,
                wallet,
                TransactionType.CONSUME,
                request.getAmount(),
                request.getDescription(),
                request.getRequestId()
        );

        return toTransactionResponse(transaction);
    }

    public WalletResponse getBalance(String customerId) {
        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for customer: " + customerId));

        return new WalletResponse(wallet.getCustomerId(), wallet.getBalance());
    }

    private Wallet getOrCreateWallet(String customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setCustomerId(customerId);
                    newWallet.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString();
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getStatus()
        );
    }
}
