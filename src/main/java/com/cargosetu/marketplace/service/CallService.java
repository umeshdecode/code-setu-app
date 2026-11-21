package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.entity.CallLog;
import com.cargosetu.marketplace.repository.CallLogRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CallService {

    private final CallLogRepository callLogRepository;
    private final MarketplaceAuditService auditService;

    @Transactional
    public GenericDataDTO initiateCall(LoggedInUser user, Long listingId, Long bookingId, Long receiverStaffId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            CallLog log = new CallLog();
            log.setCallerStaffId(user.getStaffId());
            log.setReceiverStaffId(receiverStaffId);
            log.setListingId(listingId);
            log.setBookingId(bookingId);
            log.setStatus("INITIATED");
            callLogRepository.save(log);

            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Call initiated");
        } catch (Exception e) {
            log.error("Error initiating call", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO endCall(Long callId, Integer durationSeconds, String status) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            CallLog log = callLogRepository.findById(callId).orElseThrow();
            log.setDurationSeconds(durationSeconds);
            log.setStatus(status);
            callLogRepository.save(log);

            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getCallHistory(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

    public GenericDataDTO getCallsForBooking(Long bookingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setDataList(callLogRepository.findByBookingId(bookingId));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
