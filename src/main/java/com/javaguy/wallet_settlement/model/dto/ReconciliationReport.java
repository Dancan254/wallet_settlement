package com.javaguy.wallet_settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.util.List;
import com.javaguy.wallet_settlement.model.entity.ReconciliationRecord;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReconciliationReport {
    private LocalDate date;
    private ReconciliationSummary summary;
    private List<ReconciliationRecord> discrepancies;
}
