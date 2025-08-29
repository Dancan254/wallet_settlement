package com.javaguy.wallet_settlement.service;

import com.javaguy.wallet_settlement.exception.InsufficientFundsException;
import com.javaguy.wallet_settlement.exception.WalletAlreadyExistsException;
import com.javaguy.wallet_settlement.exception.WalletNotFoundException;
import com.javaguy.wallet_settlement.model.dto.ConsumeRequest;
import com.javaguy.wallet_settlement.model.dto.CreateWalletRequest;
import com.javaguy.wallet_settlement.model.dto.TopUpRequest;
import com.javaguy.wallet_settlement.model.dto.WalletResponse;
import com.javaguy.wallet_settlement.model.entity.Transaction;
import com.javaguy.wallet_settlement.model.entity.Wallet;
import com.javaguy.wallet_settlement.model.enums.TransactionStatus;
import com.javaguy.wallet_settlement.model.enums.TransactionType;
import com.javaguy.wallet_settlement.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private WalletService walletService;

    private String customerId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        customerId = "CUST_TEST";
        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setCustomerId(customerId);
        wallet.setBalance(BigDecimal.valueOf(500.00));
    }

    @Test
    void createWallet_Success() {
        CreateWalletRequest request = new CreateWalletRequest(customerId);
        Wallet newWallet = new Wallet();
        newWallet.setCustomerId(customerId);
        newWallet.setBalance(BigDecimal.ZERO);
        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        WalletResponse response = walletService.createWallet(request);

        assertNotNull(response);
        assertEquals(customerId, response.getCustomerId());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        verify(walletRepository, times(1)).findByCustomerId(customerId);
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void createWallet_WalletAlreadyExistsException() {
        CreateWalletRequest request = new CreateWalletRequest(customerId);
        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(wallet));

        assertThrows(WalletAlreadyExistsException.class, () -> walletService.createWallet(request));
        verify(walletRepository, times(1)).findByCustomerId(customerId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void topUp_Success() {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setDescription("Test Top-up");
        request.setRequestId("req-topup-1");

        when(transactionService.findByRequestId(anyString())).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionService.createTransaction(anyString(), any(Wallet.class), any(TransactionType.class), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(Transaction.builder()
                        .transactionId("TXN-123")
                        .type(TransactionType.TOPUP)
                        .amount(request.getAmount())
                        .description(request.getDescription())
                        .status(TransactionStatus.COMPLETED)
                        .build());

        walletService.topUp(customerId, request);

        assertEquals(BigDecimal.valueOf(600.00), wallet.getBalance());
        verify(walletRepository, times(1)).save(wallet);
        verify(transactionService, times(1)).createTransaction(anyString(), any(Wallet.class), any(TransactionType.class), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void topUp_Idempotency() {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setDescription("Test Top-up");
        request.setRequestId("req-topup-1");

        Transaction existingTransaction = Transaction.builder()
                .transactionId("TXN-EXISTING")
                .type(TransactionType.TOPUP)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionService.findByRequestId(request.getRequestId())).thenReturn(Optional.of(existingTransaction));

        walletService.topUp(customerId, request);

        verify(transactionService, times(1)).findByRequestId(request.getRequestId());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionService, never()).createTransaction(anyString(), any(Wallet.class), any(TransactionType.class), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void consume_Success() {
        ConsumeRequest request = new ConsumeRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setDescription("Test Consume");
        request.setRequestId("req-consume-1");

        when(transactionService.findByRequestId(anyString())).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdWithLock(customerId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionService.createTransaction(anyString(), any(Wallet.class), any(TransactionType.class), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(Transaction.builder()
                        .transactionId("TXN-456")
                        .type(TransactionType.CONSUME)
                        .amount(request.getAmount())
                        .description(request.getDescription())
                        .status(TransactionStatus.COMPLETED)
                        .build());

        walletService.consume(customerId, request);

        assertEquals(BigDecimal.valueOf(450.00), wallet.getBalance());
        verify(walletRepository, times(1)).findByCustomerIdWithLock(customerId);
        verify(walletRepository, times(1)).save(wallet);
        verify(transactionService, times(1)).createTransaction(anyString(), any(Wallet.class), any(TransactionType.class), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void consume_InsufficientFundsException() {
        ConsumeRequest request = new ConsumeRequest();
        request.setAmount(BigDecimal.valueOf(600.00));
        request.setDescription("Test Consume");
        request.setRequestId("req-consume-1");

        when(transactionService.findByRequestId(anyString())).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerIdWithLock(customerId)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientFundsException.class, () -> walletService.consume(customerId, request));
        verify(walletRepository, times(1)).findByCustomerIdWithLock(customerId);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionService, never()).createTransaction(anyString(), any(Wallet.class), any(TransactionType.class), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void consume_Idempotency() {
        ConsumeRequest request = new ConsumeRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setDescription("Test Consume");
        request.setRequestId("req-consume-1");

        Transaction existingTransaction = Transaction.builder()
                .transactionId("TXN-EXISTING")
                .type(TransactionType.CONSUME)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionService.findByRequestId(request.getRequestId())).thenReturn(Optional.of(existingTransaction));

        walletService.consume(customerId, request);

        verify(transactionService, times(1)).findByRequestId(request.getRequestId());
        verify(walletRepository, never()).findByCustomerIdWithLock(anyString());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionService, never()).createTransaction(anyString(), any(Wallet.class), any(TransactionType.class), any(BigDecimal.class), anyString(), anyString());
    }

    @Test
    void getBalance_Success() {
        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getBalance(customerId);

        assertNotNull(response);
        assertEquals(customerId, response.getCustomerId());
        assertEquals(BigDecimal.valueOf(500.00), response.getBalance());
        verify(walletRepository, times(1)).findByCustomerId(customerId);
    }

    @Test
    void getBalance_WalletNotFoundException() {
        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getBalance(customerId));
        verify(walletRepository, times(1)).findByCustomerId(customerId);
    }
}
