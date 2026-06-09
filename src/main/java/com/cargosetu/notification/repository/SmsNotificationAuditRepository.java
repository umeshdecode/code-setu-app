package com.cargosetu.notification.repository;

import com.cargosetu.notification.model.SmsNotificationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsNotificationAuditRepository extends JpaRepository<SmsNotificationAudit, Long> {
    Optional<SmsNotificationAudit> findByReferenceIdAndMobileNumber(String referenceId, String mobileNumber);
}
