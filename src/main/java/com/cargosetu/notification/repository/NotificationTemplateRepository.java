package com.cargosetu.notification.repository;

import com.cargosetu.notification.model.NotificationTemplate;
import com.cargosetu.notification.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByTemplateCodeAndChannel(String templateCode, NotificationChannel channel);
    Optional<NotificationTemplate> findByTemplateCodeAndChannelAndIsActiveTrue(String templateCode, NotificationChannel channel);
}
