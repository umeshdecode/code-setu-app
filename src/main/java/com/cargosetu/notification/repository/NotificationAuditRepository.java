package com.cargosetu.notification.repository;

import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationAuditRepository extends JpaRepository<NotificationAudit, Long> {
    List<NotificationAudit> findByEventId(UUID eventId);
    Optional<NotificationAudit> findByEventIdAndRecipientAndChannel(UUID eventId, String recipient, NotificationChannel channel);
}
