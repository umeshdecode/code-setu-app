package com.cargosetu.client.cardekho;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CardekhoClientImpl implements CardekhoClient {

    private final RestClient restClient;

    public CardekhoClientImpl() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl("https://trucks.cardekho.com")
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "application/json, text/plain, */*")
                .build();
    }

    @Override
    public List<CardekhoModelDto> getModelsByBrand(String brandSlug) {
        if (brandSlug == null || brandSlug.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedSlug = brandSlug.toLowerCase().trim();
        long startTime = System.currentTimeMillis();
        try {
            log.info("Requesting CarDekho brand models API for brand: {}", normalizedSlug);

            List<CardekhoModelDto> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/brand-models")
                            .queryParam("brand", normalizedSlug)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("CarDekho error response for brand {}: {}", normalizedSlug, resp.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<List<CardekhoModelDto>>() {});

            long duration = System.currentTimeMillis() - startTime;
            int count = response != null ? response.size() : 0;
            log.info("CarDekho response received: brand={}, modelsCount={}, duration={}ms",
                    normalizedSlug, count, duration);

            return response != null ? response : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch models from CarDekho for brand {}: {}", normalizedSlug, e.getMessage());
            return Collections.emptyList();
        }
    }
}
