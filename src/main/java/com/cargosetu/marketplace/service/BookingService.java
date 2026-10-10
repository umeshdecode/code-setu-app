package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.AssignVehicleDriverRequest;
import com.cargosetu.marketplace.dto.CreateBookingRequest;
import com.cargosetu.marketplace.entity.BookingItem;
import com.cargosetu.marketplace.entity.MarketplaceBooking;
import com.cargosetu.marketplace.entity.MarketplaceListing;
import com.cargosetu.marketplace.entity.TruckPost;
import com.cargosetu.marketplace.mapper.BookingItemMapper;
import com.cargosetu.marketplace.mapper.BookingMapper;
import com.cargosetu.marketplace.repository.BookingItemRepository;
import com.cargosetu.marketplace.repository.MarketplaceBookingRepository;
import com.cargosetu.marketplace.repository.MarketplaceListingRepository;
import com.cargosetu.marketplace.repository.MarketplaceOfferRepository;
import com.cargosetu.marketplace.repository.TruckPostRepository;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {

    private final MarketplaceBookingRepository bookingRepository;
    private final MarketplaceListingRepository listingRepository;
    private final TruckPostRepository truckPostRepository;
    private final MarketplaceOfferRepository offerRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingMapper bookingMapper;
    private final BookingItemMapper bookingItemMapper;
    private final PaymentService paymentService;
    private final MarketplaceTripService tripService;
    private final MarketplaceAuditService auditService;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public GenericDataDTO createBooking(LoggedInUser user, Long listingId, CreateBookingRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceListing listing = listingRepository.findByIdWithLock(listingId).orElse(null);
            if (listing == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Listing not found");
                return response;
            }

            MarketplaceBooking booking = new MarketplaceBooking();
            long count = bookingRepository.count() + 1;
            booking.setBookingNumber(String.format("BK-%s-%04d", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), count));
            booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.PENDING_PAYMENT);
            
            if (com.cargosetu.marketplace.enums.ListingType.LOAD_POST.equals(listing.getType())) {
                booking.setShipperCompanyId(listing.getCompanyId());
                booking.setTransporterCompanyId(user.getCompanyId());
            } else {
                booking.setShipperCompanyId(user.getCompanyId());
                booking.setTransporterCompanyId(listing.getCompanyId());
                
                TruckPost truckPost = truckPostRepository.findByListingId(listingId).orElse(null);
                if (truckPost != null) {
                    double requestedCapacity = request.getRequestedCapacity() != null ? request.getRequestedCapacity() : 0.0;
                    if (truckPost.getAvailableCapacity() < requestedCapacity) {
                        response.setResponseCode(CommonStatusCode.FAIL);
                        response.setResponseMessege("Insufficient capacity");
                        return response;
                    }
                    truckPost.setAvailableCapacity(truckPost.getAvailableCapacity() - requestedCapacity);
                    truckPostRepository.save(truckPost);
                }
            }

            booking.setListing(listing);
            booking = bookingRepository.save(booking);

            BookingItem item = new BookingItem();
            item.setBookingId(booking.getId());
            bookingItemRepository.save(item);

            listing.setStatus(com.cargosetu.marketplace.enums.ListingStatus.BOOKED);
            listingRepository.save(listing);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceBooking", booking.getId(), "CREATE", null, "PENDING");

            response.setData(bookingMapper.toDto(booking));
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Booking created");
        } catch (Exception e) {
            log.error("Error creating booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to create booking");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO confirmBooking(Long bookingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null) {
                booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                
                tripService.createTripFromBooking(booking);
                
                response.setResponseCode(CommonStatusCode.SUCCESS);
            }
        } catch (Exception e) {
            log.error("Error confirming booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO cancelBooking(LoggedInUser user, Long bookingId, com.cargosetu.marketplace.dto.CancellationRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null) {
                booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                response.setResponseCode(CommonStatusCode.SUCCESS);
            }
        } catch (Exception e) {
            log.error("Error cancelling booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getBooking(LoggedInUser user, Long bookingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setData(bookingMapper.toDto(bookingRepository.findById(bookingId).orElse(null)));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getMyBookings(LoggedInUser user, String status) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            com.cargosetu.marketplace.enums.BookingStatus bookingStatus = com.cargosetu.marketplace.enums.BookingStatus.valueOf(status);
            java.util.List<MarketplaceBooking> bookings = new java.util.ArrayList<>(bookingRepository.findByShipperCompanyIdAndStatus(user.getCompanyId(), bookingStatus));
            bookings.addAll(bookingRepository.findByTransporterCompanyIdAndStatus(user.getCompanyId(), bookingStatus));
            response.setDataList(bookings);
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO assignVehicleDriver(LoggedInUser user, Long bookingId, AssignVehicleDriverRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null) {
                booking.setVehicleId(request.getVehicleId());
                booking.setDriverId(request.getDriverId());
                bookingRepository.save(booking);
                response.setResponseCode(CommonStatusCode.SUCCESS);
            }
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
