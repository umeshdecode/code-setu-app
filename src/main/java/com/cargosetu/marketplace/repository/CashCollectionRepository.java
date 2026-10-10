package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.CashCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashCollectionRepository extends JpaRepository<CashCollection, Long> {
    List<CashCollection> findByBookingId(Long bookingId);
    List<CashCollection> findByReceiverTypeAndReceiverIdAndStatus(String receiverType, Long receiverId, String status);
    List<CashCollection> findByStatus(String status);
}
