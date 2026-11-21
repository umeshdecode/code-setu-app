package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.CancellationRequest;
import com.cargosetu.marketplace.entity.Cancellation;
import com.cargosetu.marketplace.entity.MarketplaceBooking;
import com.cargosetu.marketplace.entity.TruckPost;
import com.cargosetu.marketplace.repository.CancellationRepository;
import com.cargosetu.marketplace.repository.MarketplaceBookingRepository;
import com.cargosetu.marketplace.repository.MarketplaceConfigRepository;
import com.cargosetu.marketplace.repository.TruckPostRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CancellationService {

    private final CancellationRepository cancellationRepository;
    private final MarketplaceBookingRepository bookingRepository;
    private final TruckPostRepository truckPostRepository;
    private final PaymentService paymentService;
    private final MarketplaceAuditService auditService;
    private final MarketplaceConfigRepository configRepository;

    @Transactional
    public GenericDataDTO cancelBooking(LoggedInUser user, Long bookingId, CancellationRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElseThrow();
            
            Cancellation cancellation = new Cancellation();
            cancellation.setBookingId(booking.getId());
            cancellation.setReasonText(request.getReasonText());
            cancellation.setCancelledByStaffId(user.getStaffId());
            cancellationRepository.save(cancellation);

            booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceBooking", bookingId, "CANCEL", null, "CANCELLED");

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Booking cancelled successfully");
        } catch (Exception e) {
            log.error("Error cancelling booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
