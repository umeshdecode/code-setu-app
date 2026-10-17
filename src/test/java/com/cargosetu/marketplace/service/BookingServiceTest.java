package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.BookingResponse;
import com.cargosetu.marketplace.dto.CreateBookingRequest;
import com.cargosetu.marketplace.entity.MarketplaceBooking;
import com.cargosetu.marketplace.entity.MarketplaceListing;
import com.cargosetu.marketplace.enums.BookingStatus;
import com.cargosetu.marketplace.enums.ListingStatus;
import com.cargosetu.marketplace.enums.ListingType;
import com.cargosetu.marketplace.enums.PaymentTermType;
import com.cargosetu.marketplace.mapper.BookingItemMapper;
import com.cargosetu.marketplace.mapper.BookingMapper;
import com.cargosetu.marketplace.repository.*;
import com.cargosetu.model.Company;
import com.cargosetu.model.Driver;
import com.cargosetu.model.Vehicle;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private MarketplaceBookingRepository bookingRepository;
    @Mock private MarketplaceListingRepository listingRepository;
    @Mock private TruckPostRepository truckPostRepository;
    @Mock private MarketplaceOfferRepository offerRepository;
    @Mock private BookingItemRepository bookingItemRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private BookingItemMapper bookingItemMapper;
    @Mock private PaymentService paymentService;
    @Mock private MarketplaceTripService tripService;
    @Mock private MarketplaceAuditService auditService;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private BookingService bookingService;

    private LoggedInUser shipperUser;
    private MarketplaceListing truckListing;

    @BeforeEach
    void setUp() {
        shipperUser = new LoggedInUser();
        shipperUser.setCompanyId(500);
        shipperUser.setStaffId(12L);

        truckListing = new MarketplaceListing();
        truckListing.setId(99L);
        truckListing.setType(ListingType.TRUCK_POST);
        truckListing.setStatus(ListingStatus.PUBLISHED);
        truckListing.setCompanyId(200);
        truckListing.setPostedByStaffId(3L);
        truckListing.setListingNumber("TK-20261010-0099");
    }

    @Test
    @DisplayName("createBooking should succeed when freight is valid and vehicle/driver belong to company")
    void createBooking_success() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .agreedFreight(BigDecimal.valueOf(35000))
                .paymentTerms(PaymentTermType.ADVANCE_PERCENT)
                .advancePercentage(30)
                .requestedCapacity(15.0)
                .vehicleId(7L)
                .driverId(9L)
                .build();

        Vehicle vehicle = new Vehicle();
        vehicle.setId(7L);
        vehicle.setCompanyId(500);
        vehicle.setRegistrationNumber("MH14XY9999");

        Driver driver = new Driver();
        driver.setId(9L);
        driver.setCompanyId(500);
        driver.setName("Ramesh Powar");
        driver.setPhoneNumber("9822001122");

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(9L)).thenReturn(Optional.of(driver));
        when(bookingRepository.save(any(MarketplaceBooking.class))).thenAnswer(i -> {
            MarketplaceBooking b = i.getArgument(0);
            b.setId(101L);
            return b;
        });

        BookingResponse mappedResponse = new BookingResponse();
        mappedResponse.setId(101L);
        mappedResponse.setAgreedFreight(request.getAgreedFreight());
        when(bookingMapper.toDto(any(MarketplaceBooking.class))).thenReturn(mappedResponse);

        Company shipperComp = new Company();
        shipperComp.setCompanyName("Shipper Express Ltd");
        Company transComp = new Company();
        transComp.setCompanyName("Fast Transit Logistics");
        when(companyRepository.findById(500)).thenReturn(Optional.of(shipperComp));
        when(companyRepository.findById(200)).thenReturn(Optional.of(transComp));

        GenericDataDTO response = bookingService.createBooking(shipperUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CREATED);
        assertThat(response.getResponseMessege()).isEqualTo("Booking created successfully");
        assertThat(truckListing.getStatus()).isEqualTo(ListingStatus.BOOKED);
        verify(bookingRepository).save(any(MarketplaceBooking.class));
        verify(bookingItemRepository).save(any());
    }

    @Test
    @DisplayName("createBooking should fail when agreed freight <= 0")
    void createBooking_failsWhenFreightZeroOrNegative() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .agreedFreight(BigDecimal.ZERO)
                .build();

        GenericDataDTO response = bookingService.createBooking(shipperUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(response.getResponseMessege()).contains("greater than zero");
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("createBooking should fail with 403 when vehicle does not belong to user company")
    void createBooking_failsWhenVehicleNotOwned() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .agreedFreight(BigDecimal.valueOf(40000))
                .vehicleId(7L)
                .build();

        Vehicle otherVehicle = new Vehicle();
        otherVehicle.setId(7L);
        otherVehicle.setCompanyId(999); // different company

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(otherVehicle));

        GenericDataDTO response = bookingService.createBooking(shipperUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        assertThat(response.getResponseMessege()).contains("does not belong to your company");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking should set status to CANCELLED and reopen listing")
    void cancelBooking_success() {
        MarketplaceBooking booking = new MarketplaceBooking();
        booking.setId(88L);
        booking.setListing(truckListing);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setShipperCompanyId(500);

        when(bookingRepository.findById(88L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        com.cargosetu.marketplace.dto.CancellationRequest cancelReq = com.cargosetu.marketplace.dto.CancellationRequest.builder()
                .reasonCode("SHIPPER_CANCELLED")
                .reasonText("Changed requirements")
                .build();
        GenericDataDTO response = bookingService.cancelBooking(shipperUser, 88L, cancelReq);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("createBooking should fail when referenced offer is PENDING or REJECTED")
    void createBooking_failsWhenOfferRejectedOrPending() {
        com.cargosetu.marketplace.entity.MarketplaceOffer pendingOffer = new com.cargosetu.marketplace.entity.MarketplaceOffer();
        pendingOffer.setId(20L);
        pendingOffer.setListing(truckListing);
        pendingOffer.setBidderCompanyId(500);
        pendingOffer.setAmount(BigDecimal.valueOf(35000));
        pendingOffer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.PENDING);

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));
        when(offerRepository.findById(20L)).thenReturn(Optional.of(pendingOffer));

        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .offerId(20L)
                .agreedFreight(BigDecimal.valueOf(35000))
                .build();

        GenericDataDTO pendingRes = bookingService.createBooking(shipperUser, 99L, request);
        assertThat(pendingRes.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(pendingRes.getResponseMessege()).contains("ACCEPTED");

        pendingOffer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.REJECTED);
        GenericDataDTO rejectedRes = bookingService.createBooking(shipperUser, 99L, request);
        assertThat(rejectedRes.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(rejectedRes.getResponseMessege()).contains("ACCEPTED");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking should fail when agreedFreight does not match accepted offer amount")
    void createBooking_failsWhenFreightMismatched() {
        com.cargosetu.marketplace.entity.MarketplaceOffer acceptedOffer = new com.cargosetu.marketplace.entity.MarketplaceOffer();
        acceptedOffer.setId(21L);
        acceptedOffer.setListing(truckListing);
        acceptedOffer.setBidderCompanyId(500);
        acceptedOffer.setAmount(BigDecimal.valueOf(38000));
        acceptedOffer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.ACCEPTED);

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));
        when(offerRepository.findById(21L)).thenReturn(Optional.of(acceptedOffer));

        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .offerId(21L)
                .agreedFreight(BigDecimal.valueOf(35000)) // mismatch
                .build();

        GenericDataDTO response = bookingService.createBooking(shipperUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(response.getResponseMessege()).contains("match the accepted offer amount");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking should fail with FORBIDDEN when offer belongs to a different bidder")
    void createBooking_failsWhenWrongBidder() {
        com.cargosetu.marketplace.entity.MarketplaceOffer acceptedOffer = new com.cargosetu.marketplace.entity.MarketplaceOffer();
        acceptedOffer.setId(22L);
        acceptedOffer.setListing(truckListing);
        acceptedOffer.setBidderCompanyId(777); // another company's accepted offer
        acceptedOffer.setAmount(BigDecimal.valueOf(35000));
        acceptedOffer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.ACCEPTED);

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));
        when(offerRepository.findById(22L)).thenReturn(Optional.of(acceptedOffer));

        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .offerId(22L)
                .agreedFreight(BigDecimal.valueOf(35000))
                .build();

        GenericDataDTO response = bookingService.createBooking(shipperUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        assertThat(response.getResponseMessege()).contains("Offer does not belong to your company");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking should fail with FORBIDDEN on unauthorized self-booking")
    void createBooking_failsWhenSelfBooking() {
        LoggedInUser ownerUser = new LoggedInUser();
        ownerUser.setCompanyId(200); // owns truckListing
        ownerUser.setStaffId(3L);

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));

        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .agreedFreight(BigDecimal.valueOf(35000))
                .build();

        GenericDataDTO response = bookingService.createBooking(ownerUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        assertThat(response.getResponseMessege()).contains("Cannot book your own listing");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking preserves remaining capacity and keeps listing PUBLISHED on partial truck booking")
    void createBooking_partialCapacityPreservesListingAvailability() {
        com.cargosetu.marketplace.entity.TruckPost truckPost = new com.cargosetu.marketplace.entity.TruckPost();
        truckPost.setId(10L);
        truckPost.setListing(truckListing);
        truckPost.setTotalCapacity(25.0);
        truckPost.setAvailableCapacity(20.0);
        truckPost.setOccupiedCapacity(5.0);
        truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.EMPTY_STANDING);

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));
        when(truckPostRepository.findByListingId(99L)).thenReturn(Optional.of(truckPost));
        when(bookingRepository.save(any(MarketplaceBooking.class))).thenAnswer(i -> {
            MarketplaceBooking b = i.getArgument(0);
            b.setId(201L);
            return b;
        });
        when(bookingMapper.toDto(any(MarketplaceBooking.class))).thenReturn(new BookingResponse());

        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .agreedFreight(BigDecimal.valueOf(20000))
                .requestedCapacity(8.0)
                .build();

        GenericDataDTO response = bookingService.createBooking(shipperUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CREATED);
        assertThat(truckPost.getAvailableCapacity()).isEqualTo(12.0);
        assertThat(truckPost.getOccupiedCapacity()).isEqualTo(13.0);
        assertThat(truckPost.getTruckStatus()).isEqualTo(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.PARTIALLY_LOADED);
        assertThat(truckListing.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        verify(truckPostRepository).save(truckPost);
    }

    @Test
    @DisplayName("createBooking marks truck and listing BOOKED when exact remaining capacity is booked")
    void createBooking_exactCapacityMarksBooked() {
        com.cargosetu.marketplace.entity.TruckPost truckPost = new com.cargosetu.marketplace.entity.TruckPost();
        truckPost.setId(10L);
        truckPost.setListing(truckListing);
        truckPost.setTotalCapacity(20.0);
        truckPost.setAvailableCapacity(12.0);
        truckPost.setOccupiedCapacity(8.0);
        truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.PARTIALLY_LOADED);

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));
        when(truckPostRepository.findByListingId(99L)).thenReturn(Optional.of(truckPost));
        when(bookingRepository.save(any(MarketplaceBooking.class))).thenAnswer(i -> {
            MarketplaceBooking b = i.getArgument(0);
            b.setId(202L);
            return b;
        });
        when(bookingMapper.toDto(any(MarketplaceBooking.class))).thenReturn(new BookingResponse());

        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .agreedFreight(BigDecimal.valueOf(28000))
                .requestedCapacity(12.0)
                .build();

        GenericDataDTO response = bookingService.createBooking(shipperUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CREATED);
        assertThat(truckPost.getAvailableCapacity()).isEqualTo(0.0);
        assertThat(truckPost.getOccupiedCapacity()).isEqualTo(20.0);
        assertThat(truckPost.getTruckStatus()).isEqualTo(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.BOOKED);
        assertThat(truckListing.getStatus()).isEqualTo(ListingStatus.BOOKED);
        verify(truckPostRepository).save(truckPost);
    }

    @Test
    @DisplayName("createBooking fails when requestedCapacity exceeds availableCapacity")
    void createBooking_insufficientCapacityFails() {
        com.cargosetu.marketplace.entity.TruckPost truckPost = new com.cargosetu.marketplace.entity.TruckPost();
        truckPost.setId(10L);
        truckPost.setListing(truckListing);
        truckPost.setTotalCapacity(20.0);
        truckPost.setAvailableCapacity(10.0);
        truckPost.setOccupiedCapacity(10.0);
        truckPost.setTruckStatus(com.cargosetu.marketplace.enums.TruckAvailabilityStatus.PARTIALLY_LOADED);

        when(listingRepository.findByIdWithLock(99L)).thenReturn(Optional.of(truckListing));
        when(truckPostRepository.findByListingId(99L)).thenReturn(Optional.of(truckPost));

        CreateBookingRequest request = CreateBookingRequest.builder()
                .listingId(99L)
                .agreedFreight(BigDecimal.valueOf(30000))
                .requestedCapacity(15.0)
                .build();

        GenericDataDTO response = bookingService.createBooking(shipperUser, 99L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(response.getResponseMessege()).contains("Insufficient available capacity");
        assertThat(truckPost.getAvailableCapacity()).isEqualTo(10.0);
        assertThat(truckListing.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        verify(bookingRepository, never()).save(any());
    }
}
