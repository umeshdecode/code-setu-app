package com.cargosetu.repo;

import com.cargosetu.model.S3BucketConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface S3BucketConfigRepository extends JpaRepository<S3BucketConfig, Long> {

    Optional<S3BucketConfig> findByConfigKeyAndIsActive(String configKey, Boolean isActive);

    Optional<S3BucketConfig> findByConfigKey(String configKey);
}
