package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.Dispute;
import com.cargosetu.marketplace.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long>, JpaSpecificationExecutor<Dispute> {
    List<Dispute> findByBookingId(Long bookingId);
    List<Dispute> findByRaisedByCompanyId(Integer companyId);
    List<Dispute> findByStatus(DisputeStatus status);
    Optional<Dispute> findByBookingIdAndStatus(Long bookingId, DisputeStatus status);
}
