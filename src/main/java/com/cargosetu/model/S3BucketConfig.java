package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DB-managed S3 bucket configuration.
 * Allows administrators to update bucket names, regions, folder prefixes and CDN URLs
 * at runtime without redeploying the application.
 *
 * Config keys (examples): DOCUMENTS, COMPANY_ASSETS, PROFILE_IMAGES
 */
@Entity
@Table(
        name = "tbl_s3_bucket_config",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"config_key"}) }
)
@Data
@EqualsAndHashCode(callSuper = false)
public class S3BucketConfig extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Logical key used in code to look up the correct bucket.
     * Examples: DOCUMENTS, COMPANY_ASSETS, PROFILE_IMAGES
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 50)
    private String configKey;

    /** Human-readable label for the admin UI */
    @Column(name = "label", length = 100)
    private String label;

    /** AWS S3 bucket name */
    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    /** AWS region, e.g. ap-south-1 */
    @Column(name = "region", nullable = false, length = 50)
    private String region;

    /**
     * Folder prefix inside the bucket (include trailing slash).
     * Example: "documents/", "assets/company/", "profiles/"
     */
    @Column(name = "folder_prefix", length = 100)
    private String folderPrefix;

    /**
     * Base public URL for this bucket/folder (CloudFront distribution or S3 public URL).
     * Example: https://cdn.cargosetu.com or https://cargosetu-docs.s3.ap-south-1.amazonaws.com
     * Leave blank if files are private (use presigned URLs instead).
     */
    @Column(name = "base_url", length = 500)
    private String baseUrl;

    /**
     * Whether files in this config are publicly accessible (true)
     * or private/presigned-url only (false).
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    /**
     * Default presigned URL expiry in minutes (used when is_public = false).
     * Default: 60 minutes
     */
    @Column(name = "presigned_expiry_minutes")
    private Integer presignedExpiryMinutes = 60;

    /** Sort order for display in admin UI */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
