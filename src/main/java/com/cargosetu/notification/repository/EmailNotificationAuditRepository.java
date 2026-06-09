package com.cargosetu.notification.repository;

import com.cargosetu.notification.model.EmailNotificationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailNotificationAuditRepository extends JpaRepository<EmailNotificationAudit, Long> {
    Optional<EmailNotificationAudit> findByReferenceIdAndRecipientEmail(String referenceId, String recipientEmail);
}
