package com.cargosetu.service;

import com.cargosetu.model.S3BucketConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.cargosetu.dto.S3UploadResult;
import com.cargosetu.utils.utillity.ImageCompressionUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generic AWS S3 service for all file operations in the CargoSetu platform.
 *
 * <p>All methods accept a {@code configKey} string (e.g. "DOCUMENTS", "COMPANY_ASSETS",
 * "PROFILE_IMAGES") that is resolved to the correct bucket/region/folder via
 * {@link S3ConfigService}, which reads configuration from the database.</p>
 *
 * <h3>Config Keys:</h3>
 * <ul>
 *   <li>{@code DOCUMENTS}       — staff/driver KYC documents (private)</li>
 *   <li>{@code COMPANY_ASSETS}  — logos, seals, stamps (can be public)</li>
 *   <li>{@code PROFILE_IMAGES}  — staff profile photos (private)</li>
 *   <li>{@code TRIP_DOCUMENTS}  — trip-related files (private)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3ConfigService s3ConfigService;

    @org.springframework.beans.factory.annotation.Value("${aws.accessKeyId:}")
    private String accessKeyId;

    @org.springframework.beans.factory.annotation.Value("${aws.secretKey:}")
    private String secretKey;

    private final java.util.concurrent.ConcurrentHashMap<String, S3Client> regionClientCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, S3Presigner> regionPresignerCache = new java.util.concurrent.ConcurrentHashMap<>();

    private S3Client getClientForConfig(S3BucketConfig config) {
        String regionStr = config.getRegion();
        if (regionStr == null || regionStr.isBlank()) {
            return s3Client;
        }
        return regionClientCache.computeIfAbsent(regionStr, r -> {
            log.info("Creating S3Client for region: {}", r);
            software.amazon.awssdk.regions.Region region = software.amazon.awssdk.regions.Region.of(r);
            software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder().region(region);
            if (accessKeyId != null && !accessKeyId.isBlank()) {
                builder.credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKeyId, secretKey)));
            } else {
                builder.credentialsProvider(software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create());
            }
            return builder.build();
        });
    }

    private S3Presigner getPresignerForConfig(S3BucketConfig config) {
        String regionStr = config.getRegion();
        if (regionStr == null || regionStr.isBlank()) {
            return s3Presigner;
        }
        return regionPresignerCache.computeIfAbsent(regionStr, r -> {
            log.info("Creating S3Presigner for region: {}", r);
            software.amazon.awssdk.regions.Region region = software.amazon.awssdk.regions.Region.of(r);
            software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder builder = S3Presigner.builder().region(region);
            if (accessKeyId != null && !accessKeyId.isBlank()) {
                builder.credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKeyId, secretKey)));
            } else {
                builder.credentialsProvider(software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create());
            }
            return builder.build();
        });
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Upload a file to S3 using the config for {@code configKey}.
     * File is placed under {@code <folderPrefix><yyyy/MM>/<uuid>-<originalName>}.
     *
     * @return S3UploadResult containing key, viewableUrl and size
     */
    public S3UploadResult uploadFile(MultipartFile file, String configKey) throws IOException {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        String key = buildObjectKey(config.getFolderPrefix(), file.getOriginalFilename());
        return doUpload(file, config, key, file.getContentType(), configKey);
    }

    /**
     * Upload a file using a custom folder path inside the configured bucket.
     *
     * @param customFolder e.g. "driver/12345/" — appended after the config's folderPrefix
     * @return S3UploadResult containing key, viewableUrl and size
     */
    public S3UploadResult uploadFile(MultipartFile file, String configKey, String customFolder) throws IOException {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        String prefix = normalizePrefix(config.getFolderPrefix()) + normalizePrefix(customFolder);
        String key = buildObjectKey(prefix, file.getOriginalFilename());
        return doUpload(file, config, key, file.getContentType(), configKey);
    }

    /**
     * Returns the public URL for a given S3 object key.
     * Uses the {@code baseUrl} from config if set, otherwise constructs the default S3 URL.
     * For private buckets, prefer {@link #generatePresignedUrl}.
     */
    public String getPublicUrl(String s3Key, String configKey) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            String base = config.getBaseUrl().stripTrailing();
            return base + "/" + s3Key;
        }
        return "https://" + config.getBucketName() + ".s3." + config.getRegion() + ".amazonaws.com/" + s3Key;
    }

    // ── Download ──────────────────────────────────────────────────────────────

    /**
     * Download a file from S3 as a byte array.
     *
     * @param s3Key     the object key returned by uploadFile
     * @param configKey bucket config key
     */
    public byte[] downloadFile(String s3Key, String configKey) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(s3Key)
                .build();
        ResponseBytes<GetObjectResponse> response = getClientForConfig(config).getObjectAsBytes(request);
        log.debug("Downloaded {} bytes from s3://{}/{}", response.asByteArray().length, config.getBucketName(), s3Key);
        return response.asByteArray();
    }

    /**
     * Download a file from S3 as a Spring {@link Resource} (for streaming responses).
     */
    public Resource downloadFileAsResource(String s3Key, String configKey) {
        byte[] bytes = downloadFile(s3Key, configKey);
        return new ByteArrayResource(bytes);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Permanently delete an object from S3.
     *
     * @param s3Key     the object key to delete
     * @param configKey bucket config key
     */
    public void deleteFile(String s3Key, String configKey) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(s3Key)
                .build();
        getClientForConfig(config).deleteObject(request);
        log.info("Deleted s3://{}/{}", config.getBucketName(), s3Key);
    }

    // ── Presigned URLs ────────────────────────────────────────────────────────

    /**
     * Generate a time-limited presigned GET URL for a private S3 object.
     *
     * @param s3Key         the object key
     * @param configKey     bucket config key
     * @param expiryMinutes how long the URL is valid (max 7 days for IAM users)
     * @return presigned HTTPS URL
     */
    public String generatePresignedUrl(String s3Key, String configKey, int expiryMinutes) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(s3Key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presigned = getPresignerForConfig(config).presignGetObject(presignRequest);
        log.debug("Generated presigned URL for s3://{}/{} expiring in {} min", config.getBucketName(), s3Key, expiryMinutes);
        return presigned.url().toString();
    }

    /**
     * Generate a presigned URL using the expiry configured in the DB config.
     */
    public String generatePresignedUrl(String s3Key, String configKey) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        int expiry = config.getPresignedExpiryMinutes() != null ? config.getPresignedExpiryMinutes() : 60;
        return generatePresignedUrl(s3Key, configKey, expiry);
    }

    /**
     * Returns the best URL for viewing a file:
     * - If the bucket config is public → returns the direct public URL
     * - If private → returns a presigned URL using the configured expiry
     */
    public String getViewableUrl(String s3Key, String configKey) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        if (Boolean.TRUE.equals(config.getIsPublic())) {
            return getPublicUrl(s3Key, configKey);
        }
        return generatePresignedUrl(s3Key, configKey);
    }

    // ── List ──────────────────────────────────────────────────────────────────

    /**
     * List all object keys under a specific prefix in the configured bucket.
     *
     * @param configKey    bucket config key
     * @param folderPrefix sub-folder to list (relative, e.g. "driver/123/")
     * @return list of S3 object keys
     */
    public List<String> listFiles(String configKey, String folderPrefix) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        String fullPrefix = normalizePrefix(config.getFolderPrefix()) + folderPrefix;
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(config.getBucketName())
                .prefix(fullPrefix)
                .build();
        ListObjectsV2Response response = getClientForConfig(config).listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    // ── Copy ──────────────────────────────────────────────────────────────────

    /**
     * Copy an S3 object within the same bucket to a new key.
     *
     * @return the destination key
     */
    public String copyFile(String sourceKey, String destKey, String configKey) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(config.getBucketName())
                .sourceKey(sourceKey)
                .destinationBucket(config.getBucketName())
                .destinationKey(destKey)
                .build();
        getClientForConfig(config).copyObject(request);
        log.info("Copied s3://{}/{} → {}", config.getBucketName(), sourceKey, destKey);
        return destKey;
    }

    // ── Check existence ───────────────────────────────────────────────────────

    /**
     * Returns true if the given S3 object key exists in the bucket.
     */
    public boolean fileExists(String s3Key, String configKey) {
        S3BucketConfig config = s3ConfigService.getConfig(configKey);
        try {
            getClientForConfig(config).headObject(HeadObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(s3Key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private S3UploadResult doUpload(MultipartFile file, S3BucketConfig config, String key, String contentType, String configKey) throws IOException {
        byte[] bytes = file.getBytes();
        
        // Compress image if it is JPG or PNG
        if (contentType != null) {
            byte[] compressedBytes = ImageCompressionUtils.compressImage(bytes, contentType);
            if (compressedBytes.length < bytes.length) {
                log.info("Image compressed from {} bytes to {} bytes", bytes.length, compressedBytes.length);
                bytes = compressedBytes;
            }
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(key)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();
        
        getClientForConfig(config).putObject(request, RequestBody.fromBytes(bytes));
        log.info("Uploaded file to s3://{}/{} ({} bytes)", config.getBucketName(), key, bytes.length);

        String viewableUrl = getViewableUrl(key, configKey);
        return new S3UploadResult(key, viewableUrl, (long) bytes.length);
    }

    /**
     * Builds a unique S3 object key:
     * {@code <prefix><yyyy/MM>/<uuid>-<sanitizedFileName>}
     */
    private String buildObjectKey(String folderPrefix, String originalFilename) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String safeName = originalFilename == null ? "file" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return normalizePrefix(folderPrefix) + datePath + "/" + uniqueId + "-" + safeName;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return "";
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }
}
