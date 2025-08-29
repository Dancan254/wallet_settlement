package com.javaguy.wallet_settlement.controller;

import com.javaguy.wallet_settlement.model.dto.ConsumeRequest;
import com.javaguy.wallet_settlement.model.dto.TopUpRequest;
import com.javaguy.wallet_settlement.model.dto.TransactionResponse;
import com.javaguy.wallet_settlement.model.dto.WalletResponse;
import com.javaguy.wallet_settlement.model.dto.CreateWalletRequest;
import com.javaguy.wallet_settlement.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing wallet operations.
 * Provides endpoints for topping up, consuming from, and checking the balance of customer wallets.
 */
@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallet Operations", description = "APIs for managing customer wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a new wallet",
               description = "Creates a new wallet for a given customer ID.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Wallet created successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid request or wallet already exists")
               })
    public ResponseEntity<WalletResponse> createWallet(
            @Parameter(description = "Request body for creating a wallet, containing the customer ID")
            @Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Initiates a top-up transaction for a customer's wallet.
     * @param customerId The unique identifier of the customer.
     * @param request The TopUpRequest containing details of the top-up amount and transaction ID.
     * @return A ResponseEntity containing the TransactionResponse, indicating the outcome of the top-up.
     */
    @PostMapping("/{customerId}/topup")
    @Operation(summary = "Top up a customer's wallet",
               description = "Increases the balance of a customer's wallet.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Wallet top-up successful"),
                   @ApiResponse(responseCode = "400", description = "Invalid request"),
                   @ApiResponse(responseCode = "404", description = "Wallet not found")
               })
    public ResponseEntity<TransactionResponse> topUp(
            @Parameter(description = "The unique identifier of the customer")
            @PathVariable String customerId,
            @Parameter(description = "Request body for topping up the wallet, including amount, description, and request ID")
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
    @Operation(summary = "Consume from a customer's wallet",
               description = "Deducts the balance from a customer's wallet. Rejects if insufficient funds.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Wallet consumption successful"),
                   @ApiResponse(responseCode = "400", description = "Invalid request or insufficient funds"),
                   @ApiResponse(responseCode = "404", description = "Wallet not found")
               })
    public ResponseEntity<TransactionResponse> consume(
            @Parameter(description = "The unique identifier of the customer")
            @PathVariable String customerId,
            @Parameter(description = "Request body for consuming from the wallet, including amount, description, and request ID")
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
    @Operation(summary = "Get customer wallet balance",
               description = "Retrieves the current balance for a specified customer wallet.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
                   @ApiResponse(responseCode = "404", description = "Wallet not found")
               })
    public ResponseEntity<WalletResponse> getBalance(
            @Parameter(description = "The unique identifier of the customer")
            @PathVariable String customerId) {
        WalletResponse response = walletService.getBalance(customerId);
        return ResponseEntity.ok(response);
    }
}

