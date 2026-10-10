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

    private final com.cargosetu.repo.CompanyRepository companyRepository;

    @Transactional
    public GenericDataDTO createBooking(LoggedInUser user, Long listingId, CreateBookingRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (request == null) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Invalid booking request");
                return response;
            }
            if (request.getAgreedFreight() == null || request.getAgreedFreight().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Agreed freight must be greater than zero");
                return response;
            }

            MarketplaceListing listing = listingRepository.findByIdWithLock(listingId).orElse(null);
            if (listing == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Listing not found");
                return response;
            }

            if (listing.getStatus() == com.cargosetu.marketplace.enums.ListingStatus.BOOKED
                    || listing.getStatus() == com.cargosetu.marketplace.enums.ListingStatus.CLOSED
                    || listing.getStatus() == com.cargosetu.marketplace.enums.ListingStatus.CANCELLED) {
                response.setResponseCode(CommonStatusCode.CONFLICT);
                response.setResponseMessege("Listing is no longer available for booking");
                return response;
            }

            // Validate Offer if present
            if (request.getOfferId() != null) {
                com.cargosetu.marketplace.entity.MarketplaceOffer offer = offerRepository.findById(request.getOfferId()).orElse(null);
                if (offer == null || !offer.getListing().getId().equals(listingId)) {
                    response.setResponseCode(CommonStatusCode.FAIL);
                    response.setResponseMessege("Offer does not belong to this listing");
                    return response;
                }
            }

            // Validate Vehicle and Driver ownership if provided
            if (request.getVehicleId() != null) {
                com.cargosetu.model.Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElse(null);
                if (vehicle == null) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Selected vehicle not found");
                    return response;
                }
                if (vehicle.getCompanyId() != null && !vehicle.getCompanyId().equals(user.getCompanyId())) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Vehicle does not belong to your company");
                    return response;
                }
            }
            if (request.getDriverId() != null) {
                com.cargosetu.model.Driver driver = driverRepository.findById(request.getDriverId()).orElse(null);
                if (driver == null) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Selected driver not found");
                    return response;
                }
                if (driver.getCompanyId() != null && !driver.getCompanyId().equals(user.getCompanyId())) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Driver does not belong to your company");
                    return response;
                }
            }

            MarketplaceBooking booking = new MarketplaceBooking();
            long count = bookingRepository.count() + 1;
            booking.setBookingNumber(String.format("BK-%s-%04d", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), count));
            booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.PENDING_PAYMENT);
            booking.setListing(listing);
            booking.setOfferId(request.getOfferId());
            booking.setVehicleId(request.getVehicleId());
            booking.setDriverId(request.getDriverId());
            booking.setAgreedFreight(request.getAgreedFreight());
            booking.setPaymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : com.cargosetu.marketplace.enums.PaymentTermType.ADVANCE_PERCENT);
            booking.setAdvancePercentage(request.getAdvancePercentage() != null ? request.getAdvancePercentage() : 50);
            booking.setBookedAt(LocalDateTime.now());
            
            if (com.cargosetu.marketplace.enums.ListingType.LOAD_POST.equals(listing.getType())) {
                booking.setShipperCompanyId(listing.getCompanyId());
                booking.setTransporterCompanyId(user.getCompanyId());
            } else {
                booking.setShipperCompanyId(user.getCompanyId());
                booking.setTransporterCompanyId(listing.getCompanyId());
                
                TruckPost truckPost = truckPostRepository.findByListingId(listingId).orElse(null);
                if (truckPost != null) {
                    double requestedCapacity = request.getRequestedCapacity() != null ? request.getRequestedCapacity() : 0.0;
                    if (truckPost.getAvailableCapacity() != null && truckPost.getAvailableCapacity() < requestedCapacity) {
                        response.setResponseCode(CommonStatusCode.FAIL);
                        response.setResponseMessege("Insufficient available capacity on truck");
                        return response;
                    }
                    if (requestedCapacity > 0 && truckPost.getAvailableCapacity() != null) {
                        truckPost.setAvailableCapacity(truckPost.getAvailableCapacity() - requestedCapacity);
                        truckPost.setOccupiedCapacity((truckPost.getOccupiedCapacity() != null ? truckPost.getOccupiedCapacity() : 0.0) + requestedCapacity);
                        if (truckPost.getAvailableCapacity() <= 0) {
                            truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.BOOKED);
                        }
                        truckPostRepository.save(truckPost);
                    }
                }
            }

            booking = bookingRepository.save(booking);

            BookingItem item = new BookingItem();
            item.setBookingId(booking.getId());
            if (request.getRequestedCapacity() != null) {
                item.setLoadWeight(request.getRequestedCapacity());
                item.setAllocatedCapacity(request.getRequestedCapacity());
            }
            bookingItemRepository.save(item);

            listing.setStatus(com.cargosetu.marketplace.enums.ListingStatus.BOOKED);
            listingRepository.save(listing);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceBooking", booking.getId(), "CREATE", null, "PENDING_PAYMENT");

            com.cargosetu.marketplace.dto.BookingResponse bookingResponse = enrichBookingResponse(booking);
            response.setData(bookingResponse);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Booking created successfully");
        } catch (Exception e) {
            log.error("Error creating booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to create booking: " + e.getMessage());
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
            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Booking not found");
                return response;
            }
            if (user != null && user.getCompanyId() != null) {
                if (!user.getCompanyId().equals(booking.getShipperCompanyId()) && !user.getCompanyId().equals(booking.getTransporterCompanyId())) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Unauthorized to view this booking");
                    return response;
                }
            }
            response.setData(enrichBookingResponse(booking));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            log.error("Error getting booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to get booking: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO getMyBookings(LoggedInUser user, String status) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            java.util.List<MarketplaceBooking> bookings;
            if (status != null && !status.isBlank()) {
                com.cargosetu.marketplace.enums.BookingStatus bookingStatus = com.cargosetu.marketplace.enums.BookingStatus.valueOf(status.trim().toUpperCase());
                bookings = new java.util.ArrayList<>(bookingRepository.findByShipperCompanyIdAndStatus(user.getCompanyId(), bookingStatus));
                bookings.addAll(bookingRepository.findByTransporterCompanyIdAndStatus(user.getCompanyId(), bookingStatus));
            } else {
                bookings = bookingRepository.findByShipperCompanyIdOrTransporterCompanyId(user.getCompanyId(), user.getCompanyId());
            }
            java.util.List<com.cargosetu.marketplace.dto.BookingResponse> dtoList = bookings.stream()
                    .map(this::enrichBookingResponse)
                    .toList();
            response.setDataList(dtoList);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Bookings retrieved successfully");
        } catch (Exception e) {
            log.error("Error fetching bookings", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to get bookings: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public GenericDataDTO assignVehicleDriver(LoggedInUser user, Long bookingId, AssignVehicleDriverRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null) {
                if (request.getVehicleId() != null) {
                    com.cargosetu.model.Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElse(null);
                    if (vehicle != null && vehicle.getCompanyId() != null && !vehicle.getCompanyId().equals(user.getCompanyId())) {
                        response.setResponseCode(CommonStatusCode.FORBIDDEN);
                        response.setResponseMessege("Vehicle does not belong to your company");
                        return response;
                    }
                }
                if (request.getDriverId() != null) {
                    com.cargosetu.model.Driver driver = driverRepository.findById(request.getDriverId()).orElse(null);
                    if (driver != null && driver.getCompanyId() != null && !driver.getCompanyId().equals(user.getCompanyId())) {
                        response.setResponseCode(CommonStatusCode.FORBIDDEN);
                        response.setResponseMessege("Driver does not belong to your company");
                        return response;
                    }
                }
                booking.setVehicleId(request.getVehicleId());
                booking.setDriverId(request.getDriverId());
                bookingRepository.save(booking);
                response.setData(enrichBookingResponse(booking));
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Vehicle and driver assigned successfully");
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Booking not found");
            }
        } catch (Exception e) {
            log.error("Error assigning vehicle/driver", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private com.cargosetu.marketplace.dto.BookingResponse enrichBookingResponse(MarketplaceBooking booking) {
        if (booking == null) return null;
        com.cargosetu.marketplace.dto.BookingResponse dto = bookingMapper.toDto(booking);
        if (booking.getShipperCompanyId() != null) {
            companyRepository.findById(booking.getShipperCompanyId())
                    .ifPresent(c -> dto.setShipperCompanyName(c.getCompanyName()));
        }
        if (booking.getTransporterCompanyId() != null) {
            companyRepository.findById(booking.getTransporterCompanyId())
                    .ifPresent(c -> dto.setTransporterCompanyName(c.getCompanyName()));
        }
        if (booking.getVehicleId() != null) {
            vehicleRepository.findById(booking.getVehicleId()).ifPresent(v -> {
                dto.setVehicleNumber(v.getRegistrationNumber());
                dto.setVehicleType(v.getVehicleType());
            });
        }
        if (booking.getDriverId() != null) {
            driverRepository.findById(booking.getDriverId()).ifPresent(d -> {
                dto.setDriverName(d.getName());
            });
        }
        if (booking.getListing() != null) {
            dto.setListingId(booking.getListing().getId());
            dto.setListingNumber(booking.getListing().getListingNumber());
            dto.setListingType(booking.getListing().getType());
            if (booking.getListing().getLoadPost() != null) {
                dto.setOriginCity(booking.getListing().getLoadPost().getOriginCity());
                dto.setDestinationCity(booking.getListing().getLoadPost().getDestinationCity());
                dto.setCargoType(booking.getListing().getLoadPost().getCargoType());
                dto.setWeight(booking.getListing().getLoadPost().getWeight());
            } else if (booking.getListing().getTruckPost() != null) {
                dto.setOriginCity(booking.getListing().getTruckPost().getCurrentCity());
                dto.setDestinationCity(booking.getListing().getTruckPost().getRouteTo());
            }
        }
        return dto;
    }
}
