package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.AssignVehicleDriverRequest;
import com.cargosetu.marketplace.dto.CreateBookingRequest;
import com.cargosetu.marketplace.entity.*;
import com.cargosetu.marketplace.enums.ListingStatus;
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
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("User company context is required");
                return response;
            }
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
            if (request.getRequestedCapacity() != null && request.getRequestedCapacity() <= 0) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Requested capacity must be greater than zero");
                return response;
            }

            MarketplaceListing listing = listingRepository.findByIdWithLock(listingId).orElse(null);
            if (listing == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Listing not found");
                return response;
            }

            if (listing.getCompanyId() != null && listing.getCompanyId().equals(user.getCompanyId())) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Cannot book your own listing");
                return response;
            }

            if (listing.getStatus() == ListingStatus.BOOKED
                    || listing.getStatus() == ListingStatus.CLOSED
                    || listing.getStatus() == ListingStatus.CANCELLED
                    || listing.getStatus() == ListingStatus.EXPIRED
                    || listing.getStatus() == ListingStatus.DRAFT) {
                response.setResponseCode(CommonStatusCode.CONFLICT);
                response.setResponseMessege("Listing is no longer available for booking");
                return response;
            }

            // Validate Offer if present, otherwise validate Direct Booking rules
            if (request.getOfferId() != null) {
                MarketplaceOffer offer = offerRepository.findById(request.getOfferId()).orElse(null);
                if (offer == null || offer.getListing() == null || !offer.getListing().getId().equals(listingId)) {
                    response.setResponseCode(CommonStatusCode.FAIL);
                    response.setResponseMessege("Offer does not belong to this listing");
                    return response;
                }
                if (offer.getStatus() != com.cargosetu.marketplace.enums.OfferStatus.ACCEPTED) {
                    response.setResponseCode(CommonStatusCode.FAIL);
                    response.setResponseMessege("Only an ACCEPTED offer can be used to create a booking");
                    return response;
                }
                Integer bidderCompanyId = resolveBidderCompanyId(offer, listing.getCompanyId());
                if (bidderCompanyId == null || !bidderCompanyId.equals(user.getCompanyId())) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Offer does not belong to your company");
                    return response;
                }
                if (offer.getAmount() == null || request.getAgreedFreight().compareTo(offer.getAmount()) != 0) {
                    response.setResponseCode(CommonStatusCode.FAIL);
                    response.setResponseMessege("Agreed freight must match the accepted offer amount");
                    return response;
                }
            } else {
                // Direct booking validations
                if (com.cargosetu.marketplace.enums.ListingType.LOAD_POST.equals(listing.getType())
                        && listing.getLoadPost() != null
                        && "FIXED".equalsIgnoreCase(listing.getLoadPost().getPriceType())
                        && listing.getLoadPost().getAskingPrice() != null
                        && listing.getLoadPost().getAskingPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    if (request.getAgreedFreight().compareTo(listing.getLoadPost().getAskingPrice()) != 0) {
                        response.setResponseCode(CommonStatusCode.FAIL);
                        response.setResponseMessege("Direct booking agreed freight must match listing fixed asking price");
                        return response;
                    }
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
            booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.PENDING_APPROVAL);
            booking.setListing(listing);
            booking.setOfferId(request.getOfferId());
            booking.setRequestedByCompanyId(user.getCompanyId());
            booking.setRequestedByStaffId(user.getStaffId());
            booking.setRequestedCapacity(request.getRequestedCapacity());
            booking.setVehicleId(request.getVehicleId());
            booking.setDriverId(request.getDriverId());
            booking.setProposedFreight(request.getAgreedFreight());
            booking.setAgreedFreight(request.getAgreedFreight());
            booking.setLastActionByCompanyId(user.getCompanyId());
            booking.setRemarks(request.getRemarks());
            booking.setPaymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : com.cargosetu.marketplace.enums.PaymentTermType.ADVANCE_PERCENT);
            booking.setAdvancePercentage(request.getAdvancePercentage() != null ? request.getAdvancePercentage() : 50);
            booking.setBookedAt(LocalDateTime.now());
            
            if (com.cargosetu.marketplace.enums.ListingType.LOAD_POST.equals(listing.getType())) {
                booking.setShipperCompanyId(listing.getCompanyId());
                booking.setTransporterCompanyId(user.getCompanyId());
                if (booking.getRequestedCapacity() == null && listing.getLoadPost() != null) {
                    booking.setRequestedCapacity(listing.getLoadPost().getWeight());
                }
                listing.setStatus(ListingStatus.BOOKED);
            } else {
                booking.setShipperCompanyId(user.getCompanyId());
                booking.setTransporterCompanyId(listing.getCompanyId());
                
                TruckPost truckPost = truckPostRepository.findByListingId(listingId).orElse(null);
                if (truckPost != null) {
                    if (truckPost.getTruckStatus() == com.cargosetu.marketplace.enums.TruckAvailabilityStatus.BOOKED
                            || truckPost.getTruckStatus() == com.cargosetu.marketplace.enums.TruckAvailabilityStatus.UNAVAILABLE) {
                        response.setResponseCode(CommonStatusCode.CONFLICT);
                        response.setResponseMessege("Truck is no longer available for booking");
                        return response;
                    }
                    if (truckPost.getAvailableCapacity() != null) {
                        double availableCap = truckPost.getAvailableCapacity();
                        if (availableCap <= 0) {
                            response.setResponseCode(CommonStatusCode.FAIL);
                            response.setResponseMessege("Insufficient available capacity on truck");
                            return response;
                        }
                        double requestedCapacity = request.getRequestedCapacity() != null ? request.getRequestedCapacity() : availableCap;
                        if (requestedCapacity > availableCap) {
                            response.setResponseCode(CommonStatusCode.FAIL);
                            response.setResponseMessege("Insufficient available capacity on truck");
                            return response;
                        }
                        booking.setRequestedCapacity(requestedCapacity);
                        double remainingCapacity = availableCap - requestedCapacity;
                        truckPost.setAvailableCapacity(Math.max(0.0, remainingCapacity));
                        truckPost.setOccupiedCapacity((truckPost.getOccupiedCapacity() != null ? truckPost.getOccupiedCapacity() : 0.0) + requestedCapacity);
                        if (remainingCapacity <= 0) {
                            truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.BOOKED);
                            listing.setStatus(ListingStatus.BOOKED);
                        } else {
                            truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.PARTIALLY_LOADED);
                            listing.setStatus(ListingStatus.PUBLISHED);
                        }
                        truckPostRepository.save(truckPost);
                    } else {
                        truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.BOOKED);
                        truckPostRepository.save(truckPost);
                        listing.setStatus(ListingStatus.BOOKED);
                    }
                } else {
                    listing.setStatus(ListingStatus.BOOKED);
                }
            }

            booking = bookingRepository.save(booking);

            BookingItem item = new BookingItem();
            item.setBookingId(booking.getId());
            if (booking.getRequestedCapacity() != null) {
                item.setLoadWeight(booking.getRequestedCapacity());
                item.setAllocatedCapacity(booking.getRequestedCapacity());
            }
            bookingItemRepository.save(item);

            listingRepository.save(listing);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceBooking", booking.getId(), "CREATE", null, "PENDING_APPROVAL");

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

    private Integer resolveBidderCompanyId(MarketplaceOffer offer, Integer listingOwnerCompanyId) {
        MarketplaceOffer current = offer;
        int depth = 0;
        while (current != null && depth < 20) {
            if (current.getBidderCompanyId() != null && !current.getBidderCompanyId().equals(listingOwnerCompanyId)) {
                return current.getBidderCompanyId();
            }
            if (current.getParentOfferId() == null) {
                break;
            }
            current = offerRepository.findById(current.getParentOfferId()).orElse(null);
            depth++;
        }
        return offer != null ? offer.getBidderCompanyId() : null;
    }

    private Integer resolveRequesterCompanyId(MarketplaceBooking booking) {
        if (booking == null) return null;
        if (booking.getRequestedByCompanyId() != null) {
            return booking.getRequestedByCompanyId();
        }
        if (booking.getListing() != null && booking.getListing().getCompanyId() != null) {
            Integer ownerId = booking.getListing().getCompanyId();
            if (ownerId.equals(booking.getShipperCompanyId())) {
                return booking.getTransporterCompanyId();
            }
            if (ownerId.equals(booking.getTransporterCompanyId())) {
                return booking.getShipperCompanyId();
            }
        }
        return booking.getShipperCompanyId();
    }

    private boolean isParticipant(Integer companyId, MarketplaceBooking booking) {
        if (companyId == null || booking == null) return false;
        return companyId.equals(booking.getShipperCompanyId())
                || companyId.equals(booking.getTransporterCompanyId())
                || (booking.getListing() != null && companyId.equals(booking.getListing().getCompanyId()));
    }

    @Transactional
    public GenericDataDTO acceptBooking(LoggedInUser user, Long bookingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Booking not found");
                return response;
            }
            if (!isParticipant(user.getCompanyId(), booking)) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized: only booking participants can accept this booking");
                return response;
            }

            com.cargosetu.marketplace.enums.BookingStatus currentStatus = booking.getStatus();
            if (currentStatus != com.cargosetu.marketplace.enums.BookingStatus.PENDING_APPROVAL
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.REQUESTED
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.COUNTERED
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.PENDING_PAYMENT) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Cannot accept booking in status " + currentStatus);
                return response;
            }

            Integer requesterId = resolveRequesterCompanyId(booking);
            Integer listingOwnerId = booking.getListing() != null ? booking.getListing().getCompanyId() : null;

            if (currentStatus == com.cargosetu.marketplace.enums.BookingStatus.PENDING_APPROVAL
                    || currentStatus == com.cargosetu.marketplace.enums.BookingStatus.REQUESTED) {
                boolean isListingOwner = listingOwnerId != null
                        ? listingOwnerId.equals(user.getCompanyId())
                        : !user.getCompanyId().equals(requesterId);
                if (!isListingOwner) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Only the listing owner can approve a pending booking request");
                    return response;
                }
            } else if (currentStatus == com.cargosetu.marketplace.enums.BookingStatus.COUNTERED) {
                if (booking.getLastActionByCompanyId() != null && booking.getLastActionByCompanyId().equals(user.getCompanyId())) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Cannot accept your own counter-offer; waiting for the other party");
                    return response;
                }
                if (booking.getCounterFreight() != null) {
                    booking.setAgreedFreight(booking.getCounterFreight());
                }
            }

            String prevStatus = currentStatus != null ? currentStatus.name() : null;
            booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.CONFIRMED);
            booking.setLastActionByCompanyId(user.getCompanyId());
            booking = bookingRepository.save(booking);

            tripService.createTripFromBooking(booking);
            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceBooking", booking.getId(), "ACCEPT", prevStatus, "CONFIRMED");

            response.setData(enrichBookingResponse(booking));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Booking request accepted and confirmed");
        } catch (Exception e) {
            log.error("Error accepting booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to accept booking: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public GenericDataDTO rejectBooking(LoggedInUser user, Long bookingId, com.cargosetu.marketplace.dto.RejectBookingRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Booking not found");
                return response;
            }
            if (!isParticipant(user.getCompanyId(), booking)) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized: only booking participants can reject this booking");
                return response;
            }

            com.cargosetu.marketplace.enums.BookingStatus currentStatus = booking.getStatus();
            if (currentStatus != com.cargosetu.marketplace.enums.BookingStatus.PENDING_APPROVAL
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.REQUESTED
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.COUNTERED
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.PENDING_PAYMENT) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Cannot reject booking in status " + currentStatus);
                return response;
            }

            Integer requesterId = resolveRequesterCompanyId(booking);
            Integer listingOwnerId = booking.getListing() != null ? booking.getListing().getCompanyId() : null;
            if (currentStatus == com.cargosetu.marketplace.enums.BookingStatus.PENDING_APPROVAL
                    || currentStatus == com.cargosetu.marketplace.enums.BookingStatus.REQUESTED) {
                boolean isListingOwner = listingOwnerId != null
                        ? listingOwnerId.equals(user.getCompanyId())
                        : !user.getCompanyId().equals(requesterId);
                if (!isListingOwner) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Only the listing owner can reject an incoming booking request");
                    return response;
                }
            }

            String prevStatus = currentStatus != null ? currentStatus.name() : null;
            booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.REJECTED);
            booking.setLastActionByCompanyId(user.getCompanyId());
            if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
                booking.setRejectionReason(request.getReason().trim());
            } else {
                booking.setRejectionReason("Declined by participant");
            }

            releaseReservedCapacity(booking);
            booking = bookingRepository.save(booking);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceBooking", booking.getId(), "REJECT", prevStatus, "REJECTED");

            response.setData(enrichBookingResponse(booking));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Booking request rejected");
        } catch (Exception e) {
            log.error("Error rejecting booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to reject booking: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public GenericDataDTO counterBooking(LoggedInUser user, Long bookingId, com.cargosetu.marketplace.dto.CounterBookingRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            if (request == null || request.getProposedFreight() == null
                    || request.getProposedFreight().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Proposed counter freight must be greater than zero");
                return response;
            }

            MarketplaceBooking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Booking not found");
                return response;
            }
            if (!isParticipant(user.getCompanyId(), booking)) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized: only booking participants can counter this booking");
                return response;
            }

            com.cargosetu.marketplace.enums.BookingStatus currentStatus = booking.getStatus();
            if (currentStatus != com.cargosetu.marketplace.enums.BookingStatus.PENDING_APPROVAL
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.REQUESTED
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.COUNTERED
                    && currentStatus != com.cargosetu.marketplace.enums.BookingStatus.PENDING_PAYMENT) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Cannot counter booking in status " + currentStatus);
                return response;
            }

            String prevStatus = currentStatus != null ? currentStatus.name() : null;
            booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.COUNTERED);
            booking.setCounterFreight(request.getProposedFreight());
            booking.setAgreedFreight(request.getProposedFreight());
            booking.setLastActionByCompanyId(user.getCompanyId());
            if (request.getPaymentTerms() != null) {
                booking.setPaymentTerms(request.getPaymentTerms());
            }
            if (request.getAdvancePercentage() != null) {
                booking.setAdvancePercentage(request.getAdvancePercentage());
            }
            if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
                booking.setRemarks(request.getRemarks().trim());
            }
            booking = bookingRepository.save(booking);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceBooking", booking.getId(), "COUNTER", prevStatus, "COUNTERED");

            response.setData(enrichBookingResponse(booking));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Counter price proposed successfully");
        } catch (Exception e) {
            log.error("Error countering booking", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to counter booking: " + e.getMessage());
        }
        return response;
    }

    private void releaseReservedCapacity(MarketplaceBooking booking) {
        if (booking == null || booking.getListing() == null) return;
        MarketplaceListing listing = booking.getListing();
        if (com.cargosetu.marketplace.enums.ListingType.TRUCK_POST.equals(listing.getType())) {
            TruckPost truckPost = truckPostRepository.findByListingId(listing.getId()).orElse(null);
            if (truckPost != null) {
                Double allocated = booking.getRequestedCapacity();
                if (allocated == null && booking.getId() != null) {
                    java.util.List<BookingItem> items = bookingItemRepository.findByBookingId(booking.getId());
                    if (items != null && !items.isEmpty() && items.get(0).getAllocatedCapacity() != null) {
                        allocated = items.get(0).getAllocatedCapacity();
                    }
                }
                if (allocated != null && allocated > 0 && truckPost.getAvailableCapacity() != null) {
                    double currentAvail = truckPost.getAvailableCapacity();
                    double totalCap = truckPost.getTotalCapacity() != null ? truckPost.getTotalCapacity() : (currentAvail + allocated);
                    double restoredAvail = Math.min(totalCap, currentAvail + allocated);
                    double currentOccupied = truckPost.getOccupiedCapacity() != null ? truckPost.getOccupiedCapacity() : 0.0;
                    double restoredOccupied = Math.max(0.0, currentOccupied - allocated);
                    truckPost.setAvailableCapacity(restoredAvail);
                    truckPost.setOccupiedCapacity(restoredOccupied);
                    if (restoredOccupied <= 0.0) {
                        truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.EMPTY_STANDING);
                    } else {
                        truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.PARTIALLY_LOADED);
                    }
                } else {
                    truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.EMPTY_STANDING);
                }
                truckPostRepository.save(truckPost);
            }
            listing.setStatus(ListingStatus.PUBLISHED);
            listingRepository.save(listing);
        } else {
            listing.setStatus(ListingStatus.PUBLISHED);
            listingRepository.save(listing);
        }
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
            if (booking == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Booking not found");
                return response;
            }
            if (user != null && user.getCompanyId() != null) {
                if (!isParticipant(user.getCompanyId(), booking)) {
                    response.setResponseCode(CommonStatusCode.FORBIDDEN);
                    response.setResponseMessege("Not authorized to cancel this booking");
                    return response;
                }
            }
            if (booking.getStatus() == com.cargosetu.marketplace.enums.BookingStatus.COMPLETED
                    || booking.getStatus() == com.cargosetu.marketplace.enums.BookingStatus.CANCELLED
                    || booking.getStatus() == com.cargosetu.marketplace.enums.BookingStatus.REJECTED) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Booking cannot be cancelled in status " + booking.getStatus());
                return response;
            }
            if (request != null && request.getReasonText() != null && !request.getReasonText().isBlank()) {
                booking.setRejectionReason(request.getReasonText().trim());
            }
            releaseReservedCapacity(booking);
            booking.setStatus(com.cargosetu.marketplace.enums.BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            response.setData(enrichBookingResponse(booking));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Booking cancelled successfully");
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
                if (!isParticipant(user.getCompanyId(), booking)) {
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
        return getMyBookings(user, status, null);
    }

    public GenericDataDTO getMyBookings(LoggedInUser user, String status, String role) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            Integer companyId = user.getCompanyId();
            java.util.List<MarketplaceBooking> rawBookings;
            if (status != null && !status.isBlank()) {
                com.cargosetu.marketplace.enums.BookingStatus bookingStatus = com.cargosetu.marketplace.enums.BookingStatus.valueOf(status.trim().toUpperCase());
                rawBookings = new java.util.ArrayList<>(bookingRepository.findByShipperCompanyIdAndStatus(companyId, bookingStatus));
                rawBookings.addAll(bookingRepository.findByTransporterCompanyIdAndStatus(companyId, bookingStatus));
            } else {
                rawBookings = bookingRepository.findByShipperCompanyIdOrTransporterCompanyId(companyId, companyId);
            }

            // Deduplicate by id while preserving order
            java.util.Map<Long, MarketplaceBooking> uniqueMap = new java.util.LinkedHashMap<>();
            if (rawBookings != null) {
                for (MarketplaceBooking b : rawBookings) {
                    if (b != null && b.getId() != null) {
                        uniqueMap.putIfAbsent(b.getId(), b);
                    }
                }
            }

            String normalizedRole = role != null ? role.trim().toUpperCase() : "ALL";
            java.util.List<com.cargosetu.marketplace.dto.BookingResponse> dtoList = uniqueMap.values().stream()
                    .filter(b -> {
                        Integer requesterId = resolveRequesterCompanyId(b);
                        Integer ownerId = b.getListing() != null ? b.getListing().getCompanyId() : null;
                        return switch (normalizedRole) {
                            case "OUTGOING", "REQUESTED_BY_ME", "MY_REQUESTS" -> companyId.equals(requesterId);
                            case "INCOMING", "RECEIVED" -> (ownerId != null && companyId.equals(ownerId))
                                    || (ownerId == null && !companyId.equals(requesterId));
                            case "SHIPPER" -> companyId.equals(b.getShipperCompanyId());
                            case "TRANSPORTER" -> companyId.equals(b.getTransporterCompanyId());
                            default -> true;
                        };
                    })
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                            int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
                            if (cmp != 0) return cmp;
                        }
                        if (a.getId() != null && b.getId() != null) {
                            return b.getId().compareTo(a.getId());
                        }
                        return 0;
                    })
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
        com.cargosetu.marketplace.dto.BookingResponse mapped = bookingMapper.toDto(booking);
        com.cargosetu.marketplace.dto.BookingResponse dto = mapped != null ? mapped : new com.cargosetu.marketplace.dto.BookingResponse();
        dto.setId(booking.getId());
        dto.setBookingNumber(booking.getBookingNumber());
        dto.setOfferId(booking.getOfferId());
        dto.setShipperCompanyId(booking.getShipperCompanyId());
        dto.setTransporterCompanyId(booking.getTransporterCompanyId());
        dto.setRequestedByCompanyId(resolveRequesterCompanyId(booking));
        dto.setRequestedByStaffId(booking.getRequestedByStaffId());
        dto.setRequestedCapacity(booking.getRequestedCapacity());
        dto.setVehicleId(booking.getVehicleId());
        dto.setDriverId(booking.getDriverId());
        dto.setProposedFreight(booking.getProposedFreight() != null ? booking.getProposedFreight() : booking.getAgreedFreight());
        dto.setAgreedFreight(booking.getAgreedFreight());
        dto.setCounterFreight(booking.getCounterFreight());
        dto.setLastActionByCompanyId(booking.getLastActionByCompanyId());
        dto.setPaymentTerms(booking.getPaymentTerms());
        dto.setAdvancePercentage(booking.getAdvancePercentage());
        dto.setStatus(booking.getStatus());
        dto.setBookedAt(booking.getBookedAt());
        dto.setRejectionReason(booking.getRejectionReason());
        dto.setRemarks(booking.getRemarks());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());

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
            dto.setListingOwnerCompanyId(booking.getListing().getCompanyId());
            dto.setListingPostedByStaffId(booking.getListing().getPostedByStaffId());
            if (booking.getListing().getLoadPost() != null) {
                LoadPost lp = booking.getListing().getLoadPost();
                dto.setOriginCity(lp.getOriginCity());
                dto.setOriginState(lp.getOriginState());
                dto.setDestinationCity(lp.getDestinationCity());
                dto.setDestinationState(lp.getDestinationState());
                dto.setCargoType(lp.getCargoType());
                dto.setWeight(lp.getWeight());
                dto.setCapacityUnit(lp.getWeightUnit());
                if (dto.getRequestedCapacity() == null) {
                    dto.setRequestedCapacity(lp.getWeight());
                }
            } else if (booking.getListing().getTruckPost() != null) {
                TruckPost tp = booking.getListing().getTruckPost();
                dto.setOriginCity(tp.getRouteFrom() != null && !tp.getRouteFrom().isBlank() ? tp.getRouteFrom() : tp.getCurrentCity());
                dto.setOriginState(tp.getRouteFromState() != null ? tp.getRouteFromState() : tp.getCurrentState());
                dto.setDestinationCity(tp.getRouteTo());
                dto.setDestinationState(tp.getRouteToState());
                dto.setCapacityUnit(tp.getCapacityUnit());
                if (dto.getWeight() == null) {
                    dto.setWeight(dto.getRequestedCapacity() != null ? dto.getRequestedCapacity() : tp.getAvailableCapacity());
                }
            }
        }
        return dto;
    }
}
