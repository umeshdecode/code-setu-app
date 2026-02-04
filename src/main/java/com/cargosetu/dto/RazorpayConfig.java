package com.cargosetu.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "razorpay.key")
@Data
public class RazorpayConfig {
    private String id;
    private String secret;
}

