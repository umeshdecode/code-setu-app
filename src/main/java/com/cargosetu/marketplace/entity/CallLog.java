package com.cargosetu.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_call_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long listingId;
    private Long bookingId;
    private Long callerStaffId;
    private Integer callerCompanyId;
    private Long receiverStaffId;
    private Integer receiverCompanyId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private String status;
    private String providerCallId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
