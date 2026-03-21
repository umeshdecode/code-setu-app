package com.cargosetu.dto;

import com.cargosetu.document.model.DocumentStatus;

import java.time.LocalDate;

public class DocumentUploadDTO {

    private Long staffDocumentId;

    private Long staffId;

    private Long documentId;

    private String fileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    private LocalDate expiryDate;

    private DocumentStatus status;

    private String remarks;

    private Long customerId;
}
