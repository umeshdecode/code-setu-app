package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {
    List<CallLog> findByListingId(Long listingId);
    List<CallLog> findByBookingId(Long bookingId);
    List<CallLog> findByCallerStaffIdOrReceiverStaffId(Long callerId, Long receiverId);
}
