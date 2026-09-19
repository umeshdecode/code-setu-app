package com.cargosetu.loadmatch.service.impl;

import com.cargosetu.loadmatch.service.LoadMatchingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly background sweep that expires any SUGGESTED/VIEWED/REQUESTED
 * LoadMatch whose expiresAt has passed. fixedDelay (rather than a cron
 * expression) is used since there is no need for wall-clock alignment - just
 * "run again roughly an hour after the previous run finished".
 */
@Component
@Slf4j
public class LoadMatchExpirySweepJob {

    private final LoadMatchingService loadMatchingService;

    public LoadMatchExpirySweepJob(LoadMatchingService loadMatchingService) {
        this.loadMatchingService = loadMatchingService;
    }

    @Scheduled(fixedDelay = 3600000)
    public void sweep() {
        try {
            loadMatchingService.sweepExpiredMatches();
        } catch (Exception e) {
            log.error("LoadMatch expiry sweep failed: {}", e.getMessage(), e);
        }
    }
}
