package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.entity.MarketplaceAuditEvent;
import com.cargosetu.marketplace.repository.MarketplaceAuditEventRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketplaceAuditService {

    private final MarketplaceAuditEventRepository auditEventRepository;

    public void logEvent(Integer companyId, Long actorStaffId, String entityType, Long entityId, String action, String oldValue, String newValue) {
        try {
            MarketplaceAuditEvent event = new MarketplaceAuditEvent();
            event.setCompanyId(companyId);
            event.setActorStaffId(actorStaffId);
            event.setEntityType(entityType);
            event.setEntityId(entityId);
            event.setAction(action);
            event.setOldValue(oldValue);
            event.setNewValue(newValue);
            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }

    public GenericDataDTO getAuditHistory(String entityType, Long entityId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<MarketplaceAuditEvent> events = auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
            response.setDataList(events);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Audit history fetched successfully");
        } catch (Exception e) {
            log.error("Error fetching audit history", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Error fetching audit history: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO getAuditByActor(Long staffId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<MarketplaceAuditEvent> events = auditEventRepository.findByActorStaffIdOrderByCreatedAtDesc(staffId);
            response.setDataList(events);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Audit history fetched successfully");
        } catch (Exception e) {
            log.error("Error fetching audit by actor", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Error fetching audit by actor: " + e.getMessage());
        }
        return response;
    }
}
