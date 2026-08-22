package com.cargosetu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 Spring configuration.
 * Creates S3Client and S3Presigner beans using credentials from application.properties
 * or environment variables (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY).
 * Falls back to DefaultCredentialsProvider (IAM role, ~/.aws/credentials) if keys are blank.
 */
@Configuration
public class S3Config {

    @Value("${aws.accessKeyId:}")
    private String accessKeyId;

    @Value("${aws.secretKey:}")
    private String secretKey;

    @Value("${aws.defaultRegion:us-east-1}")
    private String defaultRegion;

    @Bean
    public S3Client s3Client() {
        Region region = Region.of(defaultRegion);
        if (accessKeyId != null && !accessKeyId.isBlank()) {
            return S3Client.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretKey)))
                    .build();
        }
        // Fall back to IAM role / env vars / ~/.aws/credentials
        return S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        Region region = Region.of(defaultRegion);
        if (accessKeyId != null && !accessKeyId.isBlank()) {
            return S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretKey)))
                    .build();
        }
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
