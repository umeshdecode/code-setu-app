package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.PaymentOrder;
import com.cargosetu.marketplace.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    List<PaymentOrder> findByBookingId(Long bookingId);
    List<PaymentOrder> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    Optional<PaymentOrder> findByIdempotencyKey(String key);
    Optional<PaymentOrder> findByProviderRef(String providerRef);
}
