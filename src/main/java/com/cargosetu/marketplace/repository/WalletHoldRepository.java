package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.WalletHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletHoldRepository extends JpaRepository<WalletHold, Long> {
    List<WalletHold> findByWalletIdAndStatus(Long walletId, String status);
    Optional<WalletHold> findByBookingIdAndStatus(Long bookingId, String status);
}
