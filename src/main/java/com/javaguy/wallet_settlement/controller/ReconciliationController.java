package com.javaguy.wallet_settlement.controller;

import com.javaguy.wallet_settlement.model.dto.ReconciliationReport;
import com.javaguy.wallet_settlement.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for managing reconciliation operations.
 * Provides endpoints for processing reconciliation files, retrieving reconciliation reports,
 * and exporting reconciliation data.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Reconciliation Operations", description = "APIs for managing transaction reconciliation")
public class ReconciliationController {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationController.class);

    private final ReconciliationService reconciliationService;

    /**
     * Processes a reconciliation file for a given date.
     * The file should contain external transaction data to be reconciled against internal records.
     * @param date The date for which to process reconciliation, in ISO_DATE format (e.g., "YYYY-MM-DD").
     * @param file The multipart file containing the reconciliation data.
     * @return A ResponseEntity indicating the success of the reconciliation process.
     */
    @PostMapping(value = "/process", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "Process external reconciliation report",
               description = "Uploads and processes an external transaction report (CSV or JSON) for reconciliation against internal records.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Reconciliation processed successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid file type or missing content type"),
                   @ApiResponse(responseCode = "500", description = "Internal server error during processing")
               })
    public ResponseEntity<String> processReconciliation(
            @Parameter(description = "The date for which to process reconciliation (YYYY-MM-DD)", required = true)
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "The reconciliation file (CSV or JSON). Must be multipart/form-data.", required = true)
            @RequestPart("file") MultipartFile file) {

        try {
            if (file.getContentType() == null) {
                throw new IllegalArgumentException("Content type of the file must be specified.");
            }
            String contentType = file.getContentType();
            if (contentType == null) {
                throw new IllegalArgumentException("Content type of the file is unexpectedly null after initial check.");
            }
            if (contentType.equals("text/csv")) {
                reconciliationService.processExternalReportCsv(file, date);
            } else if (contentType.equals("application/json")) {
                reconciliationService.processExternalReportJson(file, date);
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + contentType);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to process external reconciliation report", e);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
    @Operation(summary = "Get reconciliation report",
               description = "Retrieves a summary of matched and mismatched transactions for a given date.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Reconciliation report retrieved successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid date format"),
                   @ApiResponse(responseCode = "404", description = "No reconciliation data found for the specified date")
               })
    public ResponseEntity<ReconciliationReport> getReconciliationReport(
            @Parameter(description = "The date for which to retrieve the report (YYYY-MM-DD)", required = true)
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
    @Operation(summary = "Export reconciliation report to CSV",
               description = "Downloads the reconciliation report for a specified date as a CSV file.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "CSV report generated and downloaded successfully"),
                   @ApiResponse(responseCode = "500", description = "Internal server error during CSV generation")
               })
    public ResponseEntity<byte[]> exportReconciliation(
            @Parameter(description = "The date for which to export the report (YYYY-MM-DD)", required = true)
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            reconciliationService.exportReconciliationToCsv(date, outputStream);
        } catch (IOException e) {
            log.error("Failed to generate CSV for reconciliation report for date {}", date, e);
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