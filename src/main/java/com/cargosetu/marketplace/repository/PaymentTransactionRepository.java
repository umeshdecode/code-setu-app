package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<PaymentTransaction> findByGatewayRef(String gatewayRef);
    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);
}
