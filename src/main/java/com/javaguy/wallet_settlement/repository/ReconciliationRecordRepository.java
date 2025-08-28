package com.javaguy.wallet_settlement.repository;


import com.javaguy.wallet_settlement.model.entity.ReconciliationRecord;
import com.javaguy.wallet_settlement.model.enums.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, Long> {

    List<ReconciliationRecord> findByReconciliationDate(LocalDate reconciliationDate);

    @Query("SELECT r FROM ReconciliationRecord r WHERE r.reconciliationDate = :date AND r.status = :status")
    List<ReconciliationRecord> findByReconciliationDateAndStatus(
            @Param("date") LocalDate reconciliationDate,
            @Param("status") ReconciliationStatus status
    );

    @Query("SELECT COUNT(r) FROM ReconciliationRecord r WHERE r.reconciliationDate = :date AND r.status = :status")
    long countByReconciliationDateAndStatus(
            @Param("date") LocalDate reconciliationDate,
            @Param("status") ReconciliationStatus status
    );

    void deleteByReconciliationDate(LocalDate reconciliationDate);
}
