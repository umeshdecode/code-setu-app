package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.WalletTransaction;
import com.cargosetu.marketplace.enums.WalletTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    List<WalletTransaction> findByWalletIdAndType(Long walletId, WalletTransactionType type);
    List<WalletTransaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
