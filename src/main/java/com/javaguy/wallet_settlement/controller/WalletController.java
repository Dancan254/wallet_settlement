package com.javaguy.wallet_settlement.controller;

import com.javaguy.wallet_settlement.model.dto.ConsumeRequest;
import com.javaguy.wallet_settlement.model.dto.TopUpRequest;
import com.javaguy.wallet_settlement.model.dto.TransactionResponse;
import com.javaguy.wallet_settlement.model.dto.WalletResponse;
import com.javaguy.wallet_settlement.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing wallet operations.
 * Provides endpoints for topping up, consuming from, and checking the balance of customer wallets.
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    /**
     * Initiates a top-up transaction for a customer's wallet.
     * @param customerId The unique identifier of the customer.
     * @param request The TopUpRequest containing details of the top-up amount and transaction ID.
     * @return A ResponseEntity containing the TransactionResponse, indicating the outcome of the top-up.
     */
    @PostMapping("/{customerId}/topup")
    public ResponseEntity<TransactionResponse> topUp(@PathVariable String customerId,
                                                     @Valid @RequestBody TopUpRequest request) {
        TransactionResponse response = walletService.topUp(customerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates a consumption transaction from a customer's wallet.
     * @param customerId The unique identifier of the customer.
     * @param request The ConsumeRequest containing details of the consumption amount, transaction ID, and description.
     * @return A ResponseEntity containing the TransactionResponse, indicating the outcome of the consumption.
     */
    @PostMapping("/{customerId}/consume")
    public ResponseEntity<TransactionResponse> consume(@PathVariable String customerId,
                                                       @Valid @RequestBody ConsumeRequest request) {
        TransactionResponse response = walletService.consume(customerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the current balance of a customer's wallet.
     * @param customerId The unique identifier of the customer.
     * @return A ResponseEntity containing the WalletResponse, which includes the customer's balance.
     */
    @GetMapping("/{customerId}/balance")
    public ResponseEntity<WalletResponse> getBalance(@PathVariable String customerId) {
        WalletResponse response = walletService.getBalance(customerId);
        return ResponseEntity.ok(response);
    }
}

