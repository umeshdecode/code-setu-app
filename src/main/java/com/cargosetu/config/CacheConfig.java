package com.cargosetu.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_VEHICLE_BRANDS = "vehicleBrands";
    public static final String CACHE_VEHICLE_MODELS_BY_BRAND = "vehicleModelsByBrand";
    public static final String CACHE_VEHICLE_MODEL_DETAILS = "vehicleModelDetails";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CACHE_VEHICLE_BRANDS,
                CACHE_VEHICLE_MODELS_BY_BRAND,
                CACHE_VEHICLE_MODEL_DETAILS
        );
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(Duration.ofHours(6))
                        .recordStats()
        );
        return manager;
    }
}
