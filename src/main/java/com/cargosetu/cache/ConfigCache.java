package com.cargosetu.cache;


import com.cargosetu.cache.dto.ClientServiceNameValueProjection;
import com.cargosetu.repo.ClientServiceRepo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConfigCache implements ApplicationRunner {
    private final ClientServiceRepo clientServiceRepo;
    public static final ConcurrentHashMap<String,String> systemConfigs = new ConcurrentHashMap<>();
    public ConfigCache(ClientServiceRepo clientServiceRepo) {
        this.clientServiceRepo = clientServiceRepo;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<ClientServiceNameValueProjection> systemConfig = clientServiceRepo.findAllNameAndValue();
        systemConfig.forEach(service ->
                systemConfigs.put(service.getName(), service.getValue())
        );
    }

    public static String getConfig(String key){
        return systemConfigs.get(key);
    }
    public static String getConfig(String key, String defaultValue) {
        return systemConfigs.getOrDefault(key, defaultValue);
    }
}
