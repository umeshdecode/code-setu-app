package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.DisputeCategory;
import com.cargosetu.marketplace.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_dispute")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;
    private Long raisedByStaffId;
    private Integer raisedByCompanyId;

    @Enumerated(EnumType.STRING)
    private DisputeCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String priority;

    @Enumerated(EnumType.STRING)
    private DisputeStatus status;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    private Long resolvedByStaffId;
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
