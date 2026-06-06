package com.cargosetu.notification.repository;

import com.cargosetu.notification.model.NotificationLog;
import com.cargosetu.notification.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByEventId(UUID eventId);
    Optional<NotificationLog> findByEventIdAndChannelAndRecipient(UUID eventId, NotificationChannel channel, String recipient);
}
