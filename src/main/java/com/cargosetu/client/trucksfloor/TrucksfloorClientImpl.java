package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.exception.VehicleCatalogueClientException;
import com.cargosetu.config.TrucksfloorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class TrucksfloorClientImpl implements TrucksfloorClient {

    private final RestClient restClient;
    private final TrucksfloorProperties properties;

    public TrucksfloorClientImpl(@Qualifier("trucksfloorRestClient") RestClient restClient,
                                 TrucksfloorProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public TrucksfloorResponse getTrucks(String brandSlug, int page) {
        int maxRetries = Math.max(1, properties.getMaxRetries());
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            long startTime = System.currentTimeMillis();
            try {
                log.info("Requesting Trucksfloor catalogue: brand={}, page={}, attempt={}/{}",
                        brandSlug, page, attempt, maxRetries);

                TrucksfloorResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/product/list/truck/{brand}")
                                .queryParam("page", page)
                                .build(brandSlug))
                        .header("Accept", "*/*")
                        .header("Content-Type", "application/json")
                        .header("language", properties.getLanguage())
                        .header("vcode", properties.getVcode())
                        .header("Origin", "https://trucksfloor.com")
                        .header("Referer", "https://trucksfloor.com/")
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, resp) -> {
                            int statusCode = resp.getStatusCode().value();
                            String statusText = resp.getStatusText();
                            long duration = System.currentTimeMillis() - startTime;
                            log.warn("Trucksfloor error response: brand={}, page={}, status={} {}, duration={}ms",
                                    brandSlug, page, statusCode, statusText, duration);
                            if (resp.getStatusCode().is5xxServerError()) {
                                throw new HttpServerErrorException(resp.getStatusCode(), statusText);
                            } else {
                                throw new HttpClientErrorException(resp.getStatusCode(), statusText);
                            }
                        })
                        .body(TrucksfloorResponse.class);

                long duration = System.currentTimeMillis() - startTime;
                int count = (response != null && response.getData() != null) ? response.getData().size() : 0;
                log.info("Trucksfloor catalogue response received: brand={}, page={}, count={}, duration={}ms",
                        brandSlug, page, count, duration);
                return response;

            } catch (HttpServerErrorException e) {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = e.getStatusCode().value();
                log.warn("Server error from Trucksfloor [{}]: brand={}, page={}, duration={}ms",
                        statusCode, brandSlug, page, duration);

                if (attempt >= maxRetries || !isTransientServerError(statusCode)) {
                    throw new VehicleCatalogueClientException(
                            "External provider server error (" + statusCode + ") for brand: " + brandSlug, statusCode, e);
                }
                sleepBackoff(attempt);

            } catch (HttpClientErrorException e) {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = e.getStatusCode().value();
                log.warn("Client error from Trucksfloor [{}]: brand={}, page={}, duration={}ms",
                        statusCode, brandSlug, page, duration);

                if (statusCode == 429 && attempt < maxRetries) {
                    sleepBackoff(attempt);
                    continue;
                }
                throw new VehicleCatalogueClientException(
                        "External provider client error (" + statusCode + ") for brand: " + brandSlug, statusCode, e);

            } catch (ResourceAccessException e) {
                long duration = System.currentTimeMillis() - startTime;
                log.warn("Connection/read timeout to Trucksfloor: brand={}, page={}, attempt={}, duration={}ms",
                        brandSlug, page, attempt, duration);

                if (attempt >= maxRetries) {
                    throw new VehicleCatalogueClientException(
                            "Connection/timeout error connecting to external provider for brand: " + brandSlug, e);
                }
                sleepBackoff(attempt);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("Unexpected error fetching Trucksfloor catalogue: brand={}, page={}, duration={}ms",
                        brandSlug, page, duration, e);
                throw new VehicleCatalogueClientException(
                        "Unexpected error calling external provider for brand: " + brandSlug, e);
            }
        }

        throw new VehicleCatalogueClientException(
                "Exhausted retries (" + maxRetries + ") fetching Trucksfloor catalogue for brand: " + brandSlug);
    }

    private boolean isTransientServerError(int statusCode) {
        return statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private void sleepBackoff(int attempt) {
        try {
            long delay = (long) Math.pow(2, attempt - 1) * 500L;
            Thread.sleep(Math.min(delay, 2000L));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
