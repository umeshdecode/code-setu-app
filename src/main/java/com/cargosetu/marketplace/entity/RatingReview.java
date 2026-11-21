package com.cargosetu.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "marketplace_rating_review",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bookingId", "reviewerCompanyId"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;
    private Integer reviewerCompanyId;
    private Long reviewerStaffId;
    private Integer revieweeCompanyId;
    private Integer overallRating;
    private Integer communicationRating;
    private Integer punctualityRating;
    private Integer professionalismRating;
    private Integer paymentRating;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
