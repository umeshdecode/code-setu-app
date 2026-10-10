package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.MarketplaceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceConfigRepository extends JpaRepository<MarketplaceConfig, Long> {
    Optional<MarketplaceConfig> findByConfigKey(String configKey);
    List<MarketplaceConfig> findByConfigKeyStartingWith(String prefix);
}
