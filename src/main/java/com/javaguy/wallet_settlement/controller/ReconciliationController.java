package com.javaguy.wallet_settlement.controller;

import com.javaguy.wallet_settlement.model.dto.ReconciliationReport;
import com.javaguy.wallet_settlement.service.ReconciliationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Slf4j
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @GetMapping("/report")
    public ResponseEntity<ReconciliationReport> getReconciliationReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Fetching reconciliation report for date: {}", date);
        ReconciliationReport report = reconciliationService.getReconciliationReport(date);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/run")
    public ResponseEntity<ReconciliationReport> runReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Running reconciliation for date: {}", date);
        ReconciliationReport report = reconciliationService.runReconciliation(date);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExternalReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            log.info("Processing uploaded file: {} for date: {}", file.getOriginalFilename(), date);
            reconciliationService.processExternalReport(file, date);
            return ResponseEntity.ok("File processed successfully");
        } catch (IOException e) {
            log.error("Error processing file", e);
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public void exportReconciliationReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=reconciliation_" + date + ".csv");
    }
}