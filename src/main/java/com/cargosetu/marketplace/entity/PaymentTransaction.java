package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.PaymentMode;
import com.cargosetu.marketplace.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "marketplace_payment_transaction")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String transactionType;

    private BigDecimal amount;

    private String direction;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(unique = true)
    private String transactionRef;

    private String gatewayRef;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
