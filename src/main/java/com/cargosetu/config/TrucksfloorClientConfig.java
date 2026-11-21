package com.cargosetu.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class TrucksfloorClientConfig {

    @Bean
    @Qualifier("trucksfloorRestClient")
    public RestClient trucksfloorRestClient(RestClient.Builder builder, TrucksfloorProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }
}
