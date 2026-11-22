package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.CreateOfferRequest;
import com.cargosetu.marketplace.entity.MarketplaceBooking;
import com.cargosetu.marketplace.entity.MarketplaceListing;
import com.cargosetu.marketplace.entity.MarketplaceOffer;
import com.cargosetu.marketplace.enums.ListingStatus;
import com.cargosetu.marketplace.enums.ListingType;
import com.cargosetu.marketplace.enums.OfferStatus;
import com.cargosetu.marketplace.mapper.OfferMapper;
import com.cargosetu.marketplace.repository.MarketplaceBookingRepository;
import com.cargosetu.marketplace.repository.MarketplaceListingRepository;
import com.cargosetu.marketplace.repository.MarketplaceOfferRepository;
import com.cargosetu.model.Staff;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the offer negotiation state machine: who may accept / counter, and that an
 * accepted offer turns into a booking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OfferServiceNegotiationTest {

    private static final int OWNER = 1;
    private static final int BIDDER = 2;
    private static final long OWNER_STAFF = 100L;
    private static final long BIDDER_STAFF = 200L;

    @Mock private MarketplaceOfferRepository offerRepository;
    @Mock private MarketplaceListingRepository listingRepository;
    @Mock private MarketplaceBookingRepository bookingRepository;
    @Mock private OfferMapper offerMapper;
    @Mock private MarketplaceAuditService auditService;
    @Mock private BookingService bookingService;
    @Mock private CompanyRepository companyRepository;
    @Mock private StaffRepository staffRepository;

    @InjectMocks private OfferService offerService;

    private MarketplaceListing listing;

    @BeforeEach
    void setUp() {
        // Parent-offer lookups default to "not found"; tests stub the ids they need.
        when(offerRepository.findById(anyLong())).thenReturn(Optional.empty());

        listing = new MarketplaceListing();
        listing.setId(10L);
        listing.setCompanyId(OWNER);
        listing.setType(ListingType.LOAD_POST);
        listing.setStatus(ListingStatus.NEGOTIATING);
        listing.setValidUntil(LocalDateTime.now().plusDays(1));
        when(listingRepository.findByIdWithLock(10L)).thenReturn(Optional.of(listing));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        when(staffRepository.findById(OWNER_STAFF)).thenReturn(Optional.of(staff(OWNER_STAFF, OWNER)));
        when(staffRepository.findById(BIDDER_STAFF)).thenReturn(Optional.of(staff(BIDDER_STAFF, BIDDER)));
    }

    @Test
    void ownerCannotAcceptTheirOwnCounterOffer() {
        MarketplaceOffer counter = offer(2L, 1L, OWNER_STAFF);
        when(offerRepository.findById(2L)).thenReturn(Optional.of(counter));

        GenericDataDTO result = offerService.acceptOffer(user(OWNER, OWNER_STAFF), 2L);

        assertEquals(CommonStatusCode.FORBIDDEN, result.getResponseCode());
        verify(bookingService, never()).createBookingFromAcceptedOffer(any(), any(), any(), any());
    }

    @Test
    void bidderAcceptingOwnersCounterCreatesBooking() {
        MarketplaceOffer counter = offer(2L, 1L, OWNER_STAFF);
        when(offerRepository.findById(2L)).thenReturn(Optional.of(counter));
        MarketplaceBooking booking = new MarketplaceBooking();
        booking.setId(55L);
        booking.setBookingNumber("BK-TEST");
        when(bookingService.createBookingFromAcceptedOffer(eq(counter), eq(listing), eq(BIDDER), any()))
                .thenReturn(booking);

        GenericDataDTO result = offerService.acceptOffer(user(BIDDER, BIDDER_STAFF), 2L);

        assertEquals(CommonStatusCode.SUCCESS, result.getResponseCode());
        assertEquals(OfferStatus.ACCEPTED, counter.getStatus());
    }

    @Test
    void ownerAcceptsOriginalBid() {
        MarketplaceOffer bid = offer(1L, null, BIDDER_STAFF);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(bid));
        MarketplaceBooking booking = new MarketplaceBooking();
        booking.setId(56L);
        when(bookingService.createBookingFromAcceptedOffer(any(), any(), any(), any())).thenReturn(booking);

        GenericDataDTO result = offerService.acceptOffer(user(OWNER, OWNER_STAFF), 1L);

        assertEquals(CommonStatusCode.SUCCESS, result.getResponseCode());
    }

    @Test
    void supersededOfferCannotBeAccepted() {
        MarketplaceOffer bid = offer(1L, null, BIDDER_STAFF);
        bid.setStatus(OfferStatus.COUNTERED);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(bid));

        GenericDataDTO result = offerService.acceptOffer(user(OWNER, OWNER_STAFF), 1L);

        assertEquals(CommonStatusCode.CONFLICT, result.getResponseCode());
    }

    @Test
    void cannotBidOnOwnListing() {
        CreateOfferRequest request = new CreateOfferRequest();
        request.setAmount(new BigDecimal("15000"));

        GenericDataDTO result = offerService.createOffer(user(OWNER, OWNER_STAFF), 10L, request);

        assertEquals(CommonStatusCode.FORBIDDEN, result.getResponseCode());
    }

    @Test
    void secondOpenBidFromSameCompanyIsRejected() {
        MarketplaceOffer existing = offer(1L, null, BIDDER_STAFF);
        when(offerRepository.findByListingIdAndStatus(10L, OfferStatus.PENDING)).thenReturn(List.of(existing));
        CreateOfferRequest request = new CreateOfferRequest();
        request.setAmount(new BigDecimal("15000"));

        GenericDataDTO result = offerService.createOffer(user(BIDDER, BIDDER_STAFF), 10L, request);

        assertEquals(CommonStatusCode.CONFLICT, result.getResponseCode());
    }

    @Test
    void expiredOfferIsMarkedExpiredInsteadOfAccepted() {
        MarketplaceOffer bid = offer(1L, null, BIDDER_STAFF);
        bid.setValidUntil(LocalDateTime.now().minusMinutes(1));
        when(offerRepository.findById(1L)).thenReturn(Optional.of(bid));

        GenericDataDTO result = offerService.acceptOffer(user(OWNER, OWNER_STAFF), 1L);

        assertEquals(CommonStatusCode.CONFLICT, result.getResponseCode());
        assertEquals(OfferStatus.EXPIRED, bid.getStatus());
    }

    // ─── helpers ───

    private MarketplaceOffer offer(Long id, Long parentId, Long authorStaffId) {
        MarketplaceOffer o = new MarketplaceOffer();
        o.setId(id);
        o.setListing(listing);
        o.setParentOfferId(parentId);
        o.setBidderCompanyId(BIDDER);
        o.setOfferedByStaffId(authorStaffId);
        o.setAmount(new BigDecimal("20000"));
        o.setStatus(OfferStatus.PENDING);
        o.setValidUntil(LocalDateTime.now().plusHours(12));
        return o;
    }

    private static Staff staff(Long staffId, Integer companyId) {
        Staff s = new Staff();
        s.setStaffId(staffId);
        s.setCompanyId(companyId);
        return s;
    }

    private static LoggedInUser user(Integer companyId, Long staffId) {
        LoggedInUser u = new LoggedInUser();
        u.setCompanyId(companyId);
        u.setStaffId(staffId);
        return u;
    }
}
