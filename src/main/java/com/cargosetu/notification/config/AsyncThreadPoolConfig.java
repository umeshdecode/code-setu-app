package com.cargosetu.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.retry.annotation.EnableRetry;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
@EnableRetry
public class AsyncThreadPoolConfig implements AsyncConfigurer {

    private final NotificationProperties properties;

    public AsyncThreadPoolConfig(NotificationProperties properties) {
        this.properties = properties;
    }

    @Override
    @Bean(name = "notificationTaskExecutor")
    public Executor getAsyncExecutor() {
        log.info("Initializing Notification Thread Pool: core={}, max={}, queue={}",
                properties.getThreadPool().getCorePoolSize(),
                properties.getThreadPool().getMaxPoolSize(),
                properties.getThreadPool().getQueueCapacity());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getThreadPool().getCorePoolSize());
        executor.setMaxPoolSize(properties.getThreadPool().getMaxPoolSize());
        executor.setQueueCapacity(properties.getThreadPool().getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadPool().getThreadNamePrefix());
        
        // CallerRunsPolicy: when queue is full, the calling thread runs the task.
        // This prevents dropping notifications under heavy load and acts as a backpressure mechanism.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) -> {
            log.error("Unexpected exception occurred in async method: {} with params: {}", 
                    method.getName(), params, ex);
        };
    }
}
