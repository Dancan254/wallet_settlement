package com.javaguy.wallet_settlement.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ConsumeRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Service type is required")
    @Size(max = 50, message = "Service type must not exceed 50 characters")
    private String serviceType;

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 100, message = "Transaction ID must not exceed 100 characters")
    private String transactionId;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private Map<String, Object> metadata;
}
