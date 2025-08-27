package com.javaguy.wallet_settlement.model.dto;

import com.javaguy.wallet_settlement.model.entity.ReconciliationRecord;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ReconciliationReport {
    private LocalDate date;
    private ReconciliationSummary summary;
    private List<ReconciliationRecord> discrepancies;
}
