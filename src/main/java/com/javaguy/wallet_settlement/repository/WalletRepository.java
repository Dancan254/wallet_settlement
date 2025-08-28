package com.javaguy.wallet_settlement.repository;

import com.javaguy.wallet_settlement.model.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository <Wallet, Long>{
    Optional<Wallet> findByCustomerId(String customerId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT w FROM Wallet w WHERE w.customerId = :customerId")
    Optional<Wallet> findByCustomerIdWithLock(@Param("customerId") String customerId);

    boolean existsByCustomerId(String customerId);
}
