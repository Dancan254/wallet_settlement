package com.javaguy.wallet_settlement.controller;

import com.javaguy.wallet_settlement.model.dto.ReconciliationReport;
import com.javaguy.wallet_settlement.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

/**
 * REST controller for managing reconciliation operations.
 * Provides endpoints for processing reconciliation files, retrieving reconciliation reports,
 * and exporting reconciliation data.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    /**
     * Processes a reconciliation file for a given date.
     * The file should contain external transaction data to be reconciled against internal records.
     * @param date The date for which to process reconciliation, in ISO_DATE format (e.g., "YYYY-MM-DD").
     * @param file The multipart file containing the reconciliation data.
     * @return A ResponseEntity indicating the success of the reconciliation process.
     */
    @PostMapping("/process")
    public ResponseEntity<String> processReconciliation(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("file") MultipartFile file) {

        try {
            reconciliationService.processExternalReport(file, date);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process external reconciliation report", e);
        }
        return ResponseEntity.ok("Reconciliation processed successfully for date: " + date);
    }

    /**
     * Retrieves a reconciliation report for a specified date.
     * The report summarizes the reconciliation status of transactions for that day.
     * @param date The date for which to retrieve the reconciliation report, in ISO_DATE format.
     * @return A ResponseEntity containing the ReconciliationReport object.
     */
    @GetMapping("/report")
    public ResponseEntity<ReconciliationReport> getReconciliationReport(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        ReconciliationReport report = reconciliationService.getReconciliationReport(date);
        return ResponseEntity.ok(report);
    }

    /**
     * Exports the reconciliation data for a given date as a CSV file.
     * @param date The date for which to export reconciliation data, in ISO_DATE format.
     * @return A ResponseEntity containing the CSV data as a byte array, with appropriate headers for file download.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReconciliation(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            reconciliationService.exportReconciliationToCsv(date, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV for reconciliation report", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "reconciliation_" + date + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }
}