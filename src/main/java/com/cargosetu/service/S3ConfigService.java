package com.cargosetu.service;

import com.cargosetu.model.S3BucketConfig;
import com.cargosetu.repo.S3BucketConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Loads S3 bucket configuration from DB (tbl_s3_bucket_config).
 * Falls back to application.properties defaults if a config key is not found in DB.
 *
 * Usage:
 *   S3BucketConfig config = s3ConfigService.getConfig("DOCUMENTS");
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ConfigService {

    private final S3BucketConfigRepository repository;

    @Value("${aws.s3.defaultBucket:cargosetu-uploads}")
    private String defaultBucket;

    @Value("${aws.defaultRegion:us-east-1}")
    private String defaultRegion;

    /**
     * Returns an active S3 bucket config for the given key.
     * If not found in DB, returns a sensible default config using application.properties values.
     *
     * @param configKey e.g. "DOCUMENTS", "COMPANY_ASSETS", "PROFILE_IMAGES"
     */
    public S3BucketConfig getConfig(String configKey) {
        Optional<S3BucketConfig> dbConfig = repository.findByConfigKeyAndIsActive(configKey, true);
        if (dbConfig.isPresent()) {
            log.debug("S3 config loaded from DB for key: {}", configKey);
            return dbConfig.get();
        }
        log.warn("S3 config not found in DB for key '{}', using application.properties defaults", configKey);
        return buildDefaultConfig(configKey);
    }

    /** Returns all active S3 configs from DB */
    public List<S3BucketConfig> getAllConfigs() {
        return repository.findAll();
    }

    /** Save or update a bucket config */
    public S3BucketConfig saveConfig(S3BucketConfig config) {
        return repository.save(config);
    }

    /** Deactivate a bucket config by ID */
    public void deactivate(Long id) {
        repository.findById(id).ifPresent(config -> {
            config.setIsActive(false);
            repository.save(config);
        });
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private S3BucketConfig buildDefaultConfig(String configKey) {
        S3BucketConfig config = new S3BucketConfig();
        config.setConfigKey(configKey);
        config.setLabel(configKey);
        config.setBucketName(defaultBucket);
        config.setRegion(defaultRegion);
        config.setFolderPrefix(configKey.toLowerCase() + "/");
        config.setIsPublic(false);
        config.setPresignedExpiryMinutes(60);
        config.setIsActive(true);
        return config;
    }
}
