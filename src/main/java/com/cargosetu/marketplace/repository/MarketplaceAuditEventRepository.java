package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.MarketplaceAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceAuditEventRepository extends JpaRepository<MarketplaceAuditEvent, Long> {
    List<MarketplaceAuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
    List<MarketplaceAuditEvent> findByActorStaffIdOrderByCreatedAtDesc(Long staffId);
    List<MarketplaceAuditEvent> findByCompanyIdOrderByCreatedAtDesc(Integer companyId);
}
