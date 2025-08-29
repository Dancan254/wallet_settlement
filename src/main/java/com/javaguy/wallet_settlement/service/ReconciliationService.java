package com.javaguy.wallet_settlement.service;

import com.javaguy.wallet_settlement.model.dto.ReconciliationReport;
import com.javaguy.wallet_settlement.model.dto.ReconciliationSummary;
import com.javaguy.wallet_settlement.model.entity.ReconciliationRecord;
import com.javaguy.wallet_settlement.model.entity.Transaction;
import com.javaguy.wallet_settlement.model.enums.ReconciliationStatus;
import com.javaguy.wallet_settlement.model.enums.TransactionType;
import com.javaguy.wallet_settlement.repository.ReconciliationRecordRepository;
import com.javaguy.wallet_settlement.repository.TransactionRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public ReconciliationReport runReconciliation(LocalDate date){
        log.info("Running reconciliation for date: {}", date);

        List<Transaction> internalTransactions = transactionRepository.findCompletedTransactionsByDate(date);
        log.info("found {} internal transactions for {}", internalTransactions.size(), date);

        //i have created mock external transactions for testing
        List<ExternalTransaction> externalTransactions = getMockExternalTransactions(internalTransactions, date);
        log.info("found {} external transactions for {}", externalTransactions.size(), date);

        List<ReconciliationRecord> reconciliationRecords = performReconciliation(
                internalTransactions, externalTransactions, date
        );
        reconciliationRecordRepository.saveAll(reconciliationRecords);
        log.info("Reconciliation completed for date: {}", date);
        return buildReconciliationReport(reconciliationRecords, date);
    }

    @Transactional
    public void processExternalReportCsv(MultipartFile file, LocalDate date) throws IOException {
        log.info("Processing external CSV report file: {} for date: {}",file.getOriginalFilename(), date);
        List<ExternalTransaction> externalTransactions = parseCSVFile(file);
        processAndSaveReconciliation(externalTransactions, date);
        log.info("Processed {} reconciliation records from CSV for date: {}", externalTransactions.size(), date);
    }

    @Transactional
    public void processExternalReportJson(MultipartFile file, LocalDate date) throws IOException {
        log.info("Processing external JSON report file: {} for date: {}",file.getOriginalFilename(), date);
        List<ExternalTransaction> externalTransactions = parseJsonFile(file);
        processAndSaveReconciliation(externalTransactions, date);
        log.info("Processed {} reconciliation records from JSON for date: {}", externalTransactions.size(), date);
    }

    @Transactional(readOnly = true)
    public ReconciliationReport getReconciliationReport(LocalDate date) {
        List<ReconciliationRecord> records = reconciliationRecordRepository.findByReconciliationDate(date);
        return buildReconciliationReport(records, date);
    }

    public void exportReconciliationToCsv(LocalDate date, OutputStream outputStream) throws IOException {
        List<ReconciliationRecord> records = reconciliationRecordRepository.findByReconciliationDate(date);

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {
            // Write header
            writer.writeNext(new String[]{
                    "Reconciliation ID", "Internal Transaction ID", "External Transaction ID",
                    "Internal Amount", "External Amount", "Status", "Discrepancy Amount", "Reason"
            });

            // Write data
            for (ReconciliationRecord record : records) {
                writer.writeNext(new String[]{
                        record.getReconciliationId(),
                        record.getInternalTransactionId() != null ? record.getInternalTransactionId() : "",
                        record.getExternalTransactionId() != null ? record.getExternalTransactionId() : "",
                        record.getInternalAmount() != null ? record.getInternalAmount().toString() : "",
                        record.getExternalAmount() != null ? record.getExternalAmount().toString() : "",
                        String.valueOf(record.getStatus()),
                        record.getDiscrepancyAmount() != null ? record.getDiscrepancyAmount().toString() : "",
                        record.getDiscrepancyReason() != null ? record.getDiscrepancyReason() : ""
                });
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledReconciliation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            runReconciliation(yesterday);
            log.info("Scheduled reconciliation completed for {}", yesterday);
        } catch (Exception e) {
            log.error("Scheduled reconciliation failed for {}", yesterday, e);
        }
    }


    private List<ReconciliationRecord> performReconciliation(
            List<Transaction> internalTransactions,
            List<ExternalTransaction> externalTransactions,
            LocalDate date) {

        // Create lookup map for internal transactions using a composite key
        Map<String, Transaction> internalMap = internalTransactions.stream()
                .collect(Collectors.toMap(
                        this::generateInternalMatchKey,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // Create lookup map for external transactions using a composite key
        Map<String, ExternalTransaction> externalMap = externalTransactions.stream()
                .collect(Collectors.toMap(
                        this::generateExternalMatchKey,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<ReconciliationRecord> records = new ArrayList<>();
        Set<String> matchedInternalKeys = new HashSet<>();
        Set<String> matchedExternalKeys = new HashSet<>();

        // Phase 1: Match internal transactions with external transactions
        for (Transaction internal : internalTransactions) {
            String internalKey = generateInternalMatchKey(internal);
            ExternalTransaction external = externalMap.get(internalKey);

            if (external != null) {
                // Found a match
                records.add(createReconciliationRecord(internal, external, date));
                matchedInternalKeys.add(internalKey);
                matchedExternalKeys.add(internalKey);
            } else {
                // Internal transaction potentially missing in external report
                // This will be handled in Phase 2 if it's not a true match
            }
        }

        // Phase 2: Identify mismatches and missing records
        // Add records for internal transactions not matched in Phase 1
        internalTransactions.stream()
                .filter(internal -> !matchedInternalKeys.contains(generateInternalMatchKey(internal)))
                .forEach(internal -> records.add(createMissingExternalRecord(internal, date)));

        // Add records for external transactions not matched in Phase 1
        externalTransactions.stream()
                .filter(external -> !matchedExternalKeys.contains(generateExternalMatchKey(external)))
                .forEach(external -> records.add(createMissingInternalRecord(external, date)));

        return records;
    }

    private ReconciliationRecord createReconciliationRecord(
            Transaction internal, ExternalTransaction external, LocalDate date) {

        ReconciliationRecord.ReconciliationRecordBuilder builder = ReconciliationRecord.builder()
                .reconciliationId(UUID.randomUUID().toString())
                .reconciliationDate(date)
                .internalTransactionId(internal.getTransactionId())
                .externalTransactionId(external.getTransactionId())
                .internalAmount(internal.getAmount())
                .externalAmount(external.getAmount());

        if (internal.getAmount().compareTo(external.getAmount()) == 0) {
            builder.status(ReconciliationStatus.MATCHED)
                    .discrepancyAmount(BigDecimal.ZERO)
                    .discrepancyReason(null);
        } else {
            builder.status(ReconciliationStatus.AMOUNT_MISMATCH)
                    .discrepancyAmount(internal.getAmount().subtract(external.getAmount()))
                    .discrepancyReason(String.format("Amount mismatch: Internal=%s, External=%s",
                            internal.getAmount(), external.getAmount()));
        }

        return builder.build();
    }

    private ReconciliationRecord createMissingExternalRecord(Transaction internal, LocalDate date) {
        return ReconciliationRecord.builder()
                .reconciliationId(UUID.randomUUID().toString())
                .reconciliationDate(date)
                .internalTransactionId(internal.getTransactionId())
                .internalAmount(internal.getAmount())
                .status(ReconciliationStatus.MISSING_EXTERNAL)
                .discrepancyAmount(null)
                .discrepancyReason("Internal transaction not found in external report")
                .build();
    }

    private ReconciliationRecord createMissingInternalRecord(ExternalTransaction external, LocalDate date) {
        return ReconciliationRecord.builder()
                .reconciliationId(UUID.randomUUID().toString())
                .reconciliationDate(date)
                .externalTransactionId(external.getTransactionId())
                .externalAmount(external.getAmount())
                .status(ReconciliationStatus.MISSING_INTERNAL)
                .discrepancyAmount(null)
                .discrepancyReason("External transaction not found in internal records")
                .build();
    }

    private ReconciliationReport buildReconciliationReport(List<ReconciliationRecord> records, LocalDate date) {
        Map<ReconciliationStatus, Long> statusCounts = records.stream()
                .collect(Collectors.groupingBy(ReconciliationRecord::getStatus, Collectors.counting()));

        BigDecimal totalAmount = records.stream()
                .filter(r -> r.getInternalAmount() != null || r.getExternalAmount() != null)
                .map(r -> r.getInternalAmount() != null ? r.getInternalAmount() : r.getExternalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal matchedAmount = records.stream()
                .filter(r -> r.getStatus() == ReconciliationStatus.MATCHED)
                .map(ReconciliationRecord::getInternalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ReconciliationRecord> discrepancies = records.stream()
                .filter(r -> r.getStatus() != ReconciliationStatus.MATCHED)
                .collect(Collectors.toList());

        ReconciliationSummary summary = ReconciliationSummary.builder()
                .totalTransactions(records.size())
                .matched(statusCounts.getOrDefault(ReconciliationStatus.MATCHED, 0L).intValue())
                .mismatched(records.size() - statusCounts.getOrDefault(ReconciliationStatus.MATCHED, 0L).intValue())
                .totalAmount(totalAmount)
                .matchedAmount(matchedAmount)
                .discrepancyAmount(totalAmount.subtract(matchedAmount))
                .build();

        return ReconciliationReport.builder()
                .date(date)
                .summary(summary)
                .discrepancies(discrepancies)
                .build();
    }

    private List<ExternalTransaction> parseCSVFile(MultipartFile file) throws IOException {
        List<ExternalTransaction> transactions = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            reader.skip(1); // Skip header row
            String[] nextLine;

            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length >= 5) { // Assuming 5 columns now: transactionId, amount, customerId, type, transactionDate
                    try {
                        ExternalTransaction transaction = ExternalTransaction.builder()
                                .transactionId(nextLine[0].trim())
                                .amount(new BigDecimal(nextLine[1].trim()))
                                .customerId(nextLine[2].trim())
                                .type(TransactionType.valueOf(nextLine[3].trim().toUpperCase()))
                                .transactionDate(LocalDate.parse(nextLine[4].trim()))
                                .build();
                        transactions.add(transaction);
                    } catch (IllegalArgumentException e) {
                        log.warn("Skipping invalid row: {}", Arrays.toString(nextLine));
                    }
                }
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        return transactions;
    }

    private List<ExternalTransaction> parseJsonFile(MultipartFile file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register Java 8 Date/Time modules
        try {
            return Arrays.asList(objectMapper.readValue(file.getBytes(), ExternalTransaction[].class));
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to parse JSON file", e);
        }
    }

    private void processAndSaveReconciliation(List<ExternalTransaction> externalTransactions, LocalDate date) {
        List<Transaction> internalTransactions = transactionRepository.findCompletedTransactionsByDate(date);

        List<ReconciliationRecord> reconciliationRecords = performReconciliation(
                internalTransactions, externalTransactions, date
        );
        reconciliationRecordRepository.saveAll(reconciliationRecords);
    }

    // Mock external transactions for demo purposes
    private List<ExternalTransaction> getMockExternalTransactions(
            List<Transaction> internalTransactions, LocalDate date) {

        List<ExternalTransaction> externalTransactions = new ArrayList<>();
        Random random = new Random();

        for (Transaction internal : internalTransactions) {
            if (random.nextDouble() < 0.9) { // 90% chance of a matching transaction
                externalTransactions.add(ExternalTransaction.builder()
                        .transactionId("EXT-" + internal.getTransactionId())
                        .amount(internal.getAmount())
                        .customerId(internal.getWallet().getCustomerId())
                        .type(internal.getType())
                        .transactionDate(date)
                        .build());
            } else if (random.nextDouble() < 0.05) { // 5% chance of amount mismatch
                BigDecimal discrepancy = internal.getAmount().multiply(new BigDecimal("0.05")); // 5% difference
                externalTransactions.add(ExternalTransaction.builder()
                        .transactionId("EXT-" + internal.getTransactionId())
                        .amount(internal.getAmount().subtract(discrepancy))
                        .customerId(internal.getWallet().getCustomerId())
                        .type(internal.getType())
                        .transactionDate(date)
                        .build());
            } else { // 5% chance of missing external (no entry added)
                // Do nothing, simulate missing external transaction
            }
        }

        // Add some missing external transactions (not present in internal)
        for (int i = 0; i < 2; i++) {
            if (random.nextDouble() < 0.1) { // 10% chance of adding a completely new external transaction
                externalTransactions.add(ExternalTransaction.builder()
                        .transactionId("EXT-MISSING-" + UUID.randomUUID().toString().substring(0, 8))
                        .amount(new BigDecimal("100.00"))
                        .customerId("CUST-" + UUID.randomUUID().toString().substring(0, 8))
                        .type(TransactionType.TOPUP)
                        .transactionDate(date)
                        .build());
            }
        }

        return externalTransactions;
    }

    // Helper class for external transaction data
    @lombok.Data
    @lombok.Builder
    public static class ExternalTransaction {
        private String transactionId;
        private BigDecimal amount;
        private String customerId;
        private TransactionType type;
        private LocalDate transactionDate;
    }

    private String generateInternalMatchKey(Transaction internal) {
        return internal.getWallet().getCustomerId() + "|" + internal.getAmount() + "|" + internal.getType() + "|" + internal.getCreatedAt().toLocalDate();
    }

    private String generateExternalMatchKey(ExternalTransaction external) {
        return external.getCustomerId() + "|" + external.getAmount() + "|" + external.getType() + "|" + external.getTransactionDate();
    }
}