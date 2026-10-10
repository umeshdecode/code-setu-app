package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.DisputeRequest;
import com.cargosetu.marketplace.dto.DisputeUpdateRequest;
import com.cargosetu.marketplace.entity.Dispute;
import com.cargosetu.marketplace.entity.DisputeEvidence;
import com.cargosetu.marketplace.mapper.DisputeMapper;
import com.cargosetu.marketplace.repository.DisputeEvidenceRepository;
import com.cargosetu.marketplace.repository.DisputeRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository evidenceRepository;
    private final DisputeMapper disputeMapper;
    private final MarketplaceAuditService auditService;

    @Transactional
    public GenericDataDTO raiseDispute(LoggedInUser user, Long bookingId, DisputeRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Dispute dispute = new Dispute();
            dispute.setBookingId(bookingId);
            dispute.setRaisedByCompanyId(user.getCompanyId());
            dispute.setStatus(com.cargosetu.marketplace.enums.DisputeStatus.OPEN);
            disputeRepository.save(dispute);
            
            response.setResponseCode(CommonStatusCode.CREATED);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getDispute(Long disputeId) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

    public GenericDataDTO getMyDisputes(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

    @Transactional
    public GenericDataDTO updateDispute(LoggedInUser user, Long disputeId, DisputeUpdateRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

    @Transactional
    public GenericDataDTO addEvidence(LoggedInUser user, Long disputeId, String evidenceType, String fileRef, String description) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Dispute dispute = disputeRepository.findById(disputeId).orElseThrow();
            DisputeEvidence evidence = new DisputeEvidence();
            evidence.setDisputeId(dispute.getId());
            evidence.setEvidenceType(evidenceType);
            evidence.setFileRef(fileRef);
            evidence.setDescription(description);
            evidenceRepository.save(evidence);
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
