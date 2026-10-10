package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    List<PaymentAllocation> findByBookingId(Long bookingId);
    List<PaymentAllocation> findByTransactionId(Long transactionId);
}
