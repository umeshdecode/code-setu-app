package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "tblm_company_favourite_templates",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_company_template_type", columnNames = {"company_id", "template_type"})
    },
    indexes = {
        @Index(name = "IDX_cft_company_type", columnList = "company_id, template_type")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyFavouriteTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "template_type", nullable = false, length = 50)
    private String templateType;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}
