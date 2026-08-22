package com.cargosetu.controller;

import com.cargosetu.dto.S3UploadResult;
import com.cargosetu.model.S3BucketConfig;
import com.cargosetu.service.S3ConfigService;
import com.cargosetu.service.S3StorageService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin endpoints for managing AWS S3 bucket configurations and performing
 * generic S3 file operations (presigned URLs, generic upload).
 *
 * Base path: /api/v2/cargosetu/s3
 */
@Slf4j
@RestController
@RequestMapping(BaseURL.BASE_URL + "/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3ConfigService s3ConfigService;
    private final S3StorageService s3StorageService;

    // ── Bucket Config Management ──────────────────────────────────────────────

    /** List all S3 bucket configurations */
    @GetMapping("/config")
    public ResponseEntity<GenericDataDTO> getAllConfigs() {
        List<S3BucketConfig> configs = s3ConfigService.getAllConfigs();
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("S3 configs fetched successfully");
        response.setDataList(configs);
        response.setTotalRecords(configs.size());
        return ResponseEntity.ok(response);
    }

    /** Get a single config by its configKey (e.g. DOCUMENTS) */
    @GetMapping("/config/{configKey}")
    public ResponseEntity<GenericDataDTO> getConfigByKey(@PathVariable String configKey) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("Config fetched");
        response.setData(config);
        return ResponseEntity.ok(response);
    }

    /** Create or update an S3 bucket config */
    @PostMapping("/config")
    public ResponseEntity<GenericDataDTO> saveConfig(@RequestBody S3BucketConfig config) {
        S3BucketConfig saved = s3ConfigService.saveConfig(config);
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("S3 config saved successfully");
        response.setData(saved);
        return ResponseEntity.ok(response);
    }

    /** Update an existing config by ID */
    @PutMapping("/config/{id}")
    public ResponseEntity<GenericDataDTO> updateConfig(@PathVariable Long id, @RequestBody S3BucketConfig config) {
        config.setId(id);
        S3BucketConfig updated = s3ConfigService.saveConfig(config);
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("S3 config updated successfully");
        response.setData(updated);
        return ResponseEntity.ok(response);
    }

    /** Deactivate (soft-delete) a config by ID */
    @DeleteMapping("/config/{id}")
    public ResponseEntity<GenericDataDTO> deactivateConfig(@PathVariable Long id) {
        s3ConfigService.deactivate(id);
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("S3 config deactivated");
        return ResponseEntity.ok(response);
    }

    // ── File Operations ───────────────────────────────────────────────────────

    /**
     * Generate a presigned URL for any S3 object.
     *
     * @param s3Key         Full S3 object key (e.g. "documents/2024/08/abc-file.pdf")
     * @param configKey     Bucket config key (e.g. "DOCUMENTS")
     * @param expiryMinutes How long the URL is valid (default 60)
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<GenericDataDTO> getPresignedUrl(
            @RequestParam String s3Key,
            @RequestParam String configKey,
            @RequestParam(defaultValue = "60") int expiryMinutes) {
        String url = s3StorageService.generatePresignedUrl(s3Key, configKey, expiryMinutes);
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("Presigned URL generated successfully");
        response.setData(url);
        return ResponseEntity.ok(response);
    }

    /**
     * Generic file upload endpoint — uploads to the specified configKey bucket.
     * Returns the S3 object key and viewable URL.
     *
     * @param file      Multipart file to upload
     * @param configKey Bucket config key (e.g. "COMPANY_ASSETS", "PROFILE_IMAGES")
     */
    @PostMapping("/upload")
    public ResponseEntity<GenericDataDTO> genericUpload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String configKey) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            S3UploadResult uploadResult = s3StorageService.uploadFile(file, configKey);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("File uploaded successfully");
            response.setData(uploadResult);
        } catch (Exception e) {
            log.error("Generic S3 upload failed for configKey={}", configKey, e);
            response.setResponseCode(CommonStatusCode.FAIL);
            response.setResponseMessege("Upload failed: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an S3 file by its object key.
     */
    @DeleteMapping("/file")
    public ResponseEntity<GenericDataDTO> deleteFile(
            @RequestParam String s3Key,
            @RequestParam String configKey) {
        s3StorageService.deleteFile(s3Key, configKey);
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("File deleted from S3");
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a file exists in S3.
     */
    @GetMapping("/file/exists")
    public ResponseEntity<GenericDataDTO> fileExists(
            @RequestParam String s3Key,
            @RequestParam String configKey) {
        boolean exists = s3StorageService.fileExists(s3Key, configKey);
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("Existence checked");
        response.setData(exists);
        return ResponseEntity.ok(response);
    }
}

