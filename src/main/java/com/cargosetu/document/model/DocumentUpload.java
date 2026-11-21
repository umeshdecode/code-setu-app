package com.cargosetu.document.model;

import com.cargosetu.customer.entity.Customer;
import com.cargosetu.model.Staff;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tblm_document_upload")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long staffDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentMaster document;

    @Column(nullable = false)
    private String fileName;

    /**
     * S3 object key (relative path inside the bucket), e.g.
     * "documents/2024/08/a1b2c3d4-aadhaar.pdf"
     * Use this key to generate presigned URLs or perform S3 operations.
     */
    @Column(name = "s3_key")
    private String s3Key;

    /**
     * S3 config key that identifies the bucket used for this file.
     * Matches a configKey in tbl_s3_bucket_config (e.g. "DOCUMENTS").
     */
    @Column(name = "s3_config_key", length = 50)
    private String s3ConfigKey;

    /**
     * Public or presigned URL for displaying/downloading this file.
     * Populated on upload; for private files this should be refreshed on access.
     */
    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    /**
     * Legacy / fallback local file path (kept for backward compatibility).
     * Will be empty for new uploads; new code uses s3Key instead.
     */
    @Column(name = "file_path")
    private String filePath;

    @Column(nullable = false)
    private String fileType;  // pdf, jpg, png

    @Column(nullable = false)
    private Long fileSize;

    @Column
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column
    private String remarks;

    @JoinColumn(name = "customer_id", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
}

