package com.cargosetu.dto;

import com.cargosetu.document.model.DocumentStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentUploadDTO {

    private Long staffDocumentId;

    private Long staffId;

    private Long documentId;

    private String fileName;

    private String filePath;

    /** S3 object key returned after upload, e.g. "documents/2024/08/abc-aadhaar.pdf" */
    private String s3Key;

    /** Logical bucket config key, e.g. "DOCUMENTS" */
    private String s3ConfigKey;

    /** Viewable URL (public URL or presigned URL) for this file */
    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private LocalDate expiryDate;

    private DocumentStatus status;

    private String remarks;

    private Long customerId;
}
