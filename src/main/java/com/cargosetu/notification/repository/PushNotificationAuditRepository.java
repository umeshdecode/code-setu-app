package com.cargosetu.notification.repository;

import com.cargosetu.notification.model.PushNotificationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PushNotificationAuditRepository extends JpaRepository<PushNotificationAudit, Long> {
    Optional<PushNotificationAudit> findByReferenceIdAndPushToken(String referenceId, String pushToken);
}
