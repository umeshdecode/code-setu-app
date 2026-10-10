package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, Long> {
    List<Cancellation> findByBookingId(Long bookingId);
    List<Cancellation> findByCancelledByCompanyId(Integer companyId);
}
