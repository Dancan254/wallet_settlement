package com.javaguy.wallet_settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaguy.wallet_settlement.model.dto.ConsumeRequest;
import com.javaguy.wallet_settlement.model.dto.CreateWalletRequest;
import com.javaguy.wallet_settlement.model.dto.TopUpRequest;
import com.javaguy.wallet_settlement.repository.TransactionRepository;
import com.javaguy.wallet_settlement.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("walletdb")
            .withUsername("wallet_user")
            .withPassword("wallet_pass");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:latest")
            .withExposedPorts(5672)
            .withAdminUser("guest")
            .withAdminPassword("guest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll(); // Clear transaction data first
        walletRepository.deleteAll(); // Then clear wallet data
    }

    @Test
    void createWallet_Success() throws Exception {
        String customerId = "CUST_INT_001";
        CreateWalletRequest request = new CreateWalletRequest(customerId);

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.balance", is(0))); // Expect 0 as integer
    }

    @Test
    void createWallet_WalletAlreadyExists() throws Exception {
        String customerId = "CUST_INT_002";
        CreateWalletRequest request = new CreateWalletRequest(customerId);

        // First creation
        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Second creation should fail
        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("WALLET_ALREADY_EXISTS")));
    }

    @Test
    void topUp_Success() throws Exception {
        String customerId = "CUST_INT_003";
        createWallet(customerId);

        TopUpRequest request = new TopUpRequest();
        request.setAmount(BigDecimal.valueOf(200.00));
        request.setDescription("Initial top-up");
        request.setRequestId(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("TOPUP")))
                .andExpect(jsonPath("$.amount", is(200.00)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verifyBalance(customerId, 200.00);
    }

    @Test
    void topUp_Idempotency() throws Exception {
        String customerId = "CUST_INT_004";
        createWallet(customerId);

        String requestId = UUID.randomUUID().toString();
        TopUpRequest request = new TopUpRequest();
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setDescription("Idempotent top-up");
        request.setRequestId(requestId);

        // First top-up
        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verifyBalance(customerId, 100.00);

        // Second top-up with same request ID should return existing transaction
        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("TOPUP")))
                .andExpect(jsonPath("$.amount", is(100.00)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verifyBalance(customerId, 100.00); // Balance should not change
    }

    @Test
    void consume_Success() throws Exception {
        String customerId = "CUST_INT_005";
        createWallet(customerId);
        topUpWallet(customerId, 500.00, "Initial fund", UUID.randomUUID().toString());

        ConsumeRequest request = new ConsumeRequest();
        request.setAmount(BigDecimal.valueOf(150.00));
        request.setDescription("Service consumption");
        request.setRequestId(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("CONSUME")))
                .andExpect(jsonPath("$.amount", is(150.00)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verifyBalance(customerId, 350.00);
    }

    @Test
    void consume_InsufficientFunds() throws Exception {
        String customerId = "CUST_INT_006";
        createWallet(customerId);
        topUpWallet(customerId, 100.00, "Initial fund", UUID.randomUUID().toString());

        ConsumeRequest request = new ConsumeRequest();
        request.setAmount(BigDecimal.valueOf(200.00));
        request.setDescription("Service consumption");
        request.setRequestId(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_FUNDS")));

        verifyBalance(customerId, 100.00); // Balance should not change
    }

    @Test
    void consume_Idempotency() throws Exception {
        String customerId = "CUST_INT_007";
        createWallet(customerId);
        topUpWallet(customerId, 500.00, "Initial fund", UUID.randomUUID().toString());

        String requestId = UUID.randomUUID().toString();
        ConsumeRequest request = new ConsumeRequest();
        request.setAmount(BigDecimal.valueOf(150.00));
        request.setDescription("Idempotent consumption");
        request.setRequestId(requestId);

        // First consume
        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verifyBalance(customerId, 350.00);

        // Second consume with same request ID should return existing transaction
        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("CONSUME")))
                .andExpect(jsonPath("$.amount", is(150.00)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verifyBalance(customerId, 350.00); // Balance should not change
    }

    @Test
    void getBalance_Success() throws Exception {
        String customerId = "CUST_INT_008";
        createWallet(customerId);
        topUpWallet(customerId, 750.00, "Funding", UUID.randomUUID().toString());
        consumeWallet(customerId, 250.00, "Payment", UUID.randomUUID().toString());

        mockMvc.perform(get("/api/v1/wallets/" + customerId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    void getBalance_WalletNotFound() throws Exception {
        String customerId = "NON_EXISTENT_CUST";

        mockMvc.perform(get("/api/v1/wallets/" + customerId + "/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("WALLET_NOT_FOUND")));
    }

    // Helper methods
    private void createWallet(String customerId) throws Exception {
        CreateWalletRequest request = new CreateWalletRequest(customerId);
        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void topUpWallet(String customerId, double amount, String description, String requestId) throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(BigDecimal.valueOf(amount));
        request.setDescription(description);
        request.setRequestId(requestId);
        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void consumeWallet(String customerId, double amount, String description, String requestId) throws Exception {
        ConsumeRequest request = new ConsumeRequest();
        request.setAmount(BigDecimal.valueOf(amount));
        request.setDescription(description);
        request.setRequestId(requestId);
        mockMvc.perform(post("/api/v1/wallets/" + customerId + "/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void verifyBalance(String customerId, double expectedBalance) throws Exception {
        mockMvc.perform(get("/api/v1/wallets/" + customerId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(expectedBalance)));
    }
}
