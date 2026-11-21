package com.cargosetu.document.service;

import com.cargosetu.document.model.DocumentUpload;
import com.cargosetu.document.repository.DocumentUploadRepository;
import com.cargosetu.dto.DocumentUploadDTO;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.mapper.DocumentUploadMapper;
import com.cargosetu.service.S3StorageService;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;

/**
 * Service for uploading and validating documents.
 * Files are stored in AWS S3 using the "DOCUMENTS" config key.
 * The S3 object key and a viewable URL are persisted in tblm_document_upload.
 */
@Slf4j
@Service
public class DocumentUploadService {

    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    /** S3 config key used for all document uploads */
    public static final String S3_CONFIG_KEY = "DOCUMENTS";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final DocumentUploadMapper documentUploadMapper;
    private final DocumentUploadRepository documentUploadRepository;
    private final S3StorageService s3StorageService;

    public DocumentUploadService(DocumentUploadMapper documentUploadMapper,
                                 DocumentUploadRepository documentUploadRepository,
                                 S3StorageService s3StorageService) {
        this.documentUploadMapper = documentUploadMapper;
        this.documentUploadRepository = documentUploadRepository;
        this.s3StorageService = s3StorageService;
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    public GenericDataDTO uploadDocument(MultipartFile file, DocumentUploadDTO dto) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            String originalName = extractOriginalFilename(file);

            String extension = extractExtension(originalName);
            if (extension == null) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Invalid file extension");
                return response;
            }

            // Upload to S3
            com.cargosetu.dto.S3UploadResult uploadResult = s3StorageService.uploadFile(file, S3_CONFIG_KEY);

            // Populate DTO
            dto.setFileName(originalName);
            dto.setFilePath("");          // legacy field — intentionally blank for S3 uploads
            dto.setS3Key(uploadResult.getS3Key());
            dto.setS3ConfigKey(S3_CONFIG_KEY);
            dto.setFileUrl(uploadResult.getFileUrl());
            dto.setFileSize(uploadResult.getFileSize());
            dto.setFileType(extension);

            DocumentUpload entity = documentUploadMapper.toEntity(dto);
            documentUploadRepository.save(entity);

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Document uploaded successfully");
            response.setData(uploadResult.getFileUrl());
            log.info("Document uploaded to S3: key={}", uploadResult.getS3Key());

        } catch (Exception e) {
            log.error("Document upload failed", e);
            throw new RuntimeException("File upload to S3 failed: " + e.getMessage(), e);
        }
        return response;
    }

    // ── Generate fresh presigned URL ─────────────────────────────────────────

    /**
     * Generate a fresh presigned URL for an already-uploaded document.
     * Call this on every GET request for private documents (presigned URLs expire).
     */
    public String getPresignedUrl(Long docUploadId) {
        DocumentUpload doc = documentUploadRepository.findById(docUploadId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + docUploadId));
        if (doc.getS3Key() == null || doc.getS3Key().isBlank()) {
            // Legacy local-path file — return stored path as-is
            return doc.getFilePath();
        }
        String configKey = doc.getS3ConfigKey() != null ? doc.getS3ConfigKey() : S3_CONFIG_KEY;
        return s3StorageService.generatePresignedUrl(doc.getS3Key(), configKey);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Delete a document record and its S3 file.
     */
    public void deleteDocument(Long docUploadId) {
        DocumentUpload doc = documentUploadRepository.findById(docUploadId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + docUploadId));
        if (doc.getS3Key() != null && !doc.getS3Key().isBlank()) {
            String configKey = doc.getS3ConfigKey() != null ? doc.getS3ConfigKey() : S3_CONFIG_KEY;
            s3StorageService.deleteFile(doc.getS3Key(), configKey);
        }
        documentUploadRepository.deleteById(docUploadId);
        log.info("Deleted document record and S3 file: id={}", docUploadId);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    public ValidationResponse validateFile(MultipartFile file) {
        ValidationResponse result = new ValidationResponse(false, "fail");

        if (Objects.isNull(file)) {
            return new ValidationResponse(false, "File is required");
        }
        if (file.isEmpty()) {
            return new ValidationResponse(false, "Please upload a valid (non-empty) file");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return new ValidationResponse(false, "File exceeds maximum allowed size of 5 MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            return new ValidationResponse(false, "Only PDF, DOC, DOCX, JPG, PNG and WEBP formats are accepted");
        }

        result.setValid(true);
        result.setMessage("success");
        return result;
    }

    public ValidationResponse validateDocumentUpload(DocumentUploadDTO requestDto) {
        if (Objects.isNull(requestDto)) {
            return new ValidationResponse(false, "Invalid request body");
        }
        if (requestDto.getDocumentId() == null || requestDto.getDocumentId() <= 0) {
            return new ValidationResponse(false, "Invalid document type");
        }
        return new ValidationResponse(true, "success");
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String extractOriginalFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) return "file";
        return Paths.get(name).getFileName().toString();
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return null;
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
