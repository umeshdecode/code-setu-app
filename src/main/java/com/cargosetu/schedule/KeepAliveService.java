package com.cargosetu.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class KeepAliveService {

    private final RestClient restClient;

    @Value("${keep-alive.base-url}")
    private String baseUrl;

    public KeepAliveService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Scheduled(fixedRate = 3, timeUnit = TimeUnit.MINUTES)
    public void keepServiceAlive() {
        try {
            String response = restClient
                    .get()
                    .uri(baseUrl + "/public/isAlive")
                    .retrieve()
                    .body(String.class);

            log.info("Keep-alive API called successfully. Response: {}", response);

        } catch (Exception e) {
            log.error("Keep-alive API call failed: {}", e.getMessage());
        }
    }
}
