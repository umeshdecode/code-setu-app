package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByCompanyIdOrderByIdDesc(Integer companyId);
    List<LedgerEntry> findByCompanyIdAndBookingId(Integer companyId, Long bookingId);
    Optional<LedgerEntry> findTopByCompanyIdOrderByIdDesc(Integer companyId);
}
