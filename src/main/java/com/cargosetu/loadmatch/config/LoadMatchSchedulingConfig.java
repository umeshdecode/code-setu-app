package com.cargosetu.loadmatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * No @Configuration class in this codebase enables scheduling yet, so a
 * minimal dedicated config is added here rather than bolting @EnableScheduling
 * onto an unrelated existing class. Backs LoadMatchExpirySweepJob.
 */
@Configuration
@EnableScheduling
public class LoadMatchSchedulingConfig {
}
