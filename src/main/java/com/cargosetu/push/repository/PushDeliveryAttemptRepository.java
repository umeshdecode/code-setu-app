package com.cargosetu.push.repository;

import com.cargosetu.push.model.PushDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushDeliveryAttemptRepository extends JpaRepository<PushDeliveryAttempt, Long> {
    List<PushDeliveryAttempt> findByAuditId(Long auditId);
}
