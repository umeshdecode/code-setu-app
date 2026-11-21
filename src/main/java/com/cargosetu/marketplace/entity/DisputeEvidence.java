package com.cargosetu.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_dispute_evidence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long disputeId;
    private String evidenceType;
    private String fileRef;
    private String description;
    private Long uploadedByStaffId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
