package com.cargosetu.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private final ThreadPool threadPool = new ThreadPool();

    @Data
    public static class ThreadPool {
        private int corePoolSize = 10;
        private int maxPoolSize = 20;
        private int queueCapacity = 2000;
        private String threadNamePrefix = "NotifWorker-";
    }
}
