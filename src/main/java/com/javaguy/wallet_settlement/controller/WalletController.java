package com.javaguy.wallet_settlement.controller;

import com.javaguy.wallet_settlement.model.dto.BalanceResponse;
import com.javaguy.wallet_settlement.model.dto.ConsumeRequest;
import com.javaguy.wallet_settlement.model.dto.TopUpRequest;
import com.javaguy.wallet_settlement.model.dto.WalletResponse;
import com.javaguy.wallet_settlement.model.entity.Wallet;
import com.javaguy.wallet_settlement.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Validated
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<Wallet> createWallet(@RequestParam @NotBlank String customerId) {
        log.info("Creating wallet for customer: {}", customerId);
        Wallet wallet = walletService.createWallet(customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @PostMapping("/{walletId}/topup")
    public ResponseEntity<WalletResponse> topUp(
            @PathVariable @NotBlank String walletId,
            @RequestBody @Valid TopUpRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Use idempotency key as transaction ID if provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            request.setTransactionId(idempotencyKey);
        }

        log.info("Top-up request for wallet: {}, amount: {}", walletId, request.getAmount());
        WalletResponse response = walletService.topUp(walletId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{walletId}/consume")
    public ResponseEntity<WalletResponse> consume(
            @PathVariable @NotBlank String walletId,
            @RequestBody @Valid ConsumeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            request.setTransactionId(idempotencyKey);
        }

        log.info("Consume request for wallet: {}, amount: {}, service: {}",
                walletId, request.getAmount(), request.getServiceType());
        WalletResponse response = walletService.consume(walletId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable @NotBlank String walletId) {
        log.debug("Balance request for wallet: {}", walletId);
        BalanceResponse response = walletService.getBalance(walletId);
        return ResponseEntity.ok(response);
    }
}

