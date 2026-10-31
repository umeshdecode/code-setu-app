package com.cargosetu.notification.repository;

import com.cargosetu.notification.model.NotificationTemplate;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByTemplateCodeAndChannel(NotificationEventType templateCode, NotificationChannel channel);
    Optional<NotificationTemplate> findByTemplateCodeAndChannelAndIsActiveTrue(NotificationEventType templateCode, NotificationChannel channel);
}
