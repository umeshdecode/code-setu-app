package com.cargosetu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "vehicle.catalogue.trucksfloor")
public class TrucksfloorProperties {

    private String baseUrl = "https://api.trucksfloor.com";
    private String language = "en";
    private String vcode = "trf";
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 15;
    private int maxRetries = 3;
    private List<String> syncBrands = new ArrayList<>(List.of("tata", "eicher", "ashok-leyland", "mahindra", "bharatbenz"));
}
