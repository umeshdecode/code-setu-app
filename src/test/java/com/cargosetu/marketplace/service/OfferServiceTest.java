package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.CreateOfferRequest;
import com.cargosetu.marketplace.dto.OfferResponse;
import com.cargosetu.marketplace.entity.MarketplaceListing;
import com.cargosetu.marketplace.entity.MarketplaceOffer;
import com.cargosetu.marketplace.enums.ListingStatus;
import com.cargosetu.marketplace.enums.OfferStatus;
import com.cargosetu.marketplace.enums.PaymentTermType;
import com.cargosetu.marketplace.mapper.OfferMapper;
import com.cargosetu.marketplace.repository.MarketplaceListingRepository;
import com.cargosetu.marketplace.repository.MarketplaceOfferRepository;
import com.cargosetu.model.Company;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.repo.StaffRepository;
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
class OfferServiceTest {

    @Mock
    private MarketplaceOfferRepository offerRepository;

    @Mock
    private MarketplaceListingRepository listingRepository;

    @Mock
    private OfferMapper offerMapper;

    @Mock
    private MarketplaceAuditService auditService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private OfferService offerService;

    private LoggedInUser bidderUser;
    private LoggedInUser listingOwnerUser;
    private MarketplaceListing listing;

    @BeforeEach
    void setUp() {
        bidderUser = new LoggedInUser();
        bidderUser.setCompanyId(200);
        bidderUser.setStaffId(5L);

        listingOwnerUser = new LoggedInUser();
        listingOwnerUser.setCompanyId(100);
        listingOwnerUser.setStaffId(2L);

        listing = new MarketplaceListing();
        listing.setId(1L);
        listing.setCompanyId(100);
        listing.setPostedByStaffId(2L);
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setListingNumber("LD-20261010-0001");
    }

    @Test
    @DisplayName("createOffer should succeed when amount > 0 and bidder != owner")
    void createOffer_success() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .amount(BigDecimal.valueOf(45000))
                .paymentTerms(PaymentTermType.ADVANCE_PERCENT)
                .message("Available for immediate pickup")
                .build();

        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(10L);
        offer.setAmount(request.getAmount());
        offer.setListing(listing);

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(offerMapper.toEntity(request)).thenReturn(offer);
        when(offerRepository.save(any(MarketplaceOffer.class))).thenAnswer(i -> {
            MarketplaceOffer o = i.getArgument(0);
            o.setId(10L);
            return o;
        });

        OfferResponse mappedResponse = new OfferResponse();
        mappedResponse.setId(10L);
        mappedResponse.setAmount(request.getAmount());
        when(offerMapper.toDto(any(MarketplaceOffer.class))).thenReturn(mappedResponse);

        Company bidderComp = new Company();
        bidderComp.setCompanyName("Express Logistics");
        lenient().when(companyRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(companyRepository.findById(200)).thenReturn(Optional.of(bidderComp));

        GenericDataDTO response = offerService.createOffer(bidderUser, 1L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.CREATED);
        assertThat(response.getResponseMessege()).isEqualTo("Offer created successfully");
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.NEGOTIATING);
        verify(offerRepository).save(any(MarketplaceOffer.class));
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("createOffer should fail when amount is zero or negative")
    void createOffer_failsWhenAmountZeroOrNegative() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .amount(BigDecimal.ZERO)
                .build();

        GenericDataDTO response = offerService.createOffer(bidderUser, 1L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(response.getResponseMessege()).contains("greater than zero");
        verifyNoInteractions(offerRepository);
    }

    @Test
    @DisplayName("createOffer should fail when bidding on own listing")
    void createOffer_failsWhenBiddingOnOwnListing() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .amount(BigDecimal.valueOf(30000))
                .build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        // Listing owner attempts to bid on their own listing
        GenericDataDTO response = offerService.createOffer(listingOwnerUser, 1L, request);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);
        assertThat(response.getResponseMessege()).contains("Cannot bid on own listing");
        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptOffer should set status to ACCEPTED")
    void acceptOffer_success() {
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(55L);
        offer.setListing(listing);
        offer.setStatus(OfferStatus.PENDING);

        when(offerRepository.findById(55L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GenericDataDTO response = offerService.acceptOffer(listingOwnerUser, 55L);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
        verify(offerRepository).save(offer);
    }

    @Test
    @DisplayName("rejectOffer should set status to REJECTED")
    void rejectOffer_success() {
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(55L);
        offer.setListing(listing);
        offer.setStatus(OfferStatus.PENDING);

        when(offerRepository.findById(55L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GenericDataDTO response = offerService.rejectOffer(listingOwnerUser, 55L);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.REJECTED);
        verify(offerRepository).save(offer);
    }

    @Test
    @DisplayName("acceptOffer should fail with FORBIDDEN when called by unrelated company or bidder")
    void acceptOffer_failsWhenUnrelatedCompanyOrBidder() {
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(55L);
        offer.setListing(listing);
        offer.setBidderCompanyId(200);
        offer.setStatus(OfferStatus.PENDING);

        when(offerRepository.findById(55L)).thenReturn(Optional.of(offer));

        LoggedInUser unrelatedUser = new LoggedInUser();
        unrelatedUser.setCompanyId(300);
        unrelatedUser.setStaffId(9L);

        GenericDataDTO unrelatedRes = offerService.acceptOffer(unrelatedUser, 55L);
        assertThat(unrelatedRes.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PENDING);

        GenericDataDTO bidderRes = offerService.acceptOffer(bidderUser, 55L);
        assertThat(bidderRes.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PENDING);

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejectOffer should fail with FORBIDDEN when called by unrelated company or bidder")
    void rejectOffer_failsWhenUnrelatedCompanyOrBidder() {
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(55L);
        offer.setListing(listing);
        offer.setBidderCompanyId(200);
        offer.setStatus(OfferStatus.PENDING);

        when(offerRepository.findById(55L)).thenReturn(Optional.of(offer));

        LoggedInUser unrelatedUser = new LoggedInUser();
        unrelatedUser.setCompanyId(300);
        unrelatedUser.setStaffId(9L);

        GenericDataDTO unrelatedRes = offerService.rejectOffer(unrelatedUser, 55L);
        assertThat(unrelatedRes.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PENDING);

        GenericDataDTO bidderRes = offerService.rejectOffer(bidderUser, 55L);
        assertThat(bidderRes.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PENDING);

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("counterOffer should fail with FORBIDDEN when called by unrelated company")
    void counterOffer_failsWhenUnrelatedCompany() {
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(55L);
        offer.setListing(listing);
        offer.setBidderCompanyId(200);
        offer.setStatus(OfferStatus.PENDING);

        when(offerRepository.findById(55L)).thenReturn(Optional.of(offer));

        LoggedInUser unrelatedUser = new LoggedInUser();
        unrelatedUser.setCompanyId(300);
        unrelatedUser.setStaffId(9L);

        com.cargosetu.marketplace.dto.CounterOfferRequest counterReq = com.cargosetu.marketplace.dto.CounterOfferRequest.builder()
                .amount(BigDecimal.valueOf(42000))
                .paymentTerms(PaymentTermType.ADVANCE_PERCENT)
                .message("Unauthorized counter")
                .build();

        GenericDataDTO response = offerService.counterOffer(unrelatedUser, 55L, counterReq);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.FORBIDDEN);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PENDING);
        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("counterOffer should succeed for legitimate negotiation participants")
    void counterOffer_succeedsForNegotiationParticipants() {
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(55L);
        offer.setListing(listing);
        offer.setBidderCompanyId(200);
        offer.setStatus(OfferStatus.PENDING);

        when(offerRepository.findById(55L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any(MarketplaceOffer.class))).thenAnswer(i -> {
            MarketplaceOffer saved = i.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(56L);
            }
            return saved;
        });

        OfferResponse mappedDto = new OfferResponse();
        mappedDto.setId(56L);
        mappedDto.setAmount(BigDecimal.valueOf(43000));
        when(offerMapper.toDto(any(MarketplaceOffer.class))).thenReturn(mappedDto);

        com.cargosetu.marketplace.dto.CounterOfferRequest counterReq = com.cargosetu.marketplace.dto.CounterOfferRequest.builder()
                .amount(BigDecimal.valueOf(43000))
                .paymentTerms(PaymentTermType.ADVANCE_PERCENT)
                .message("Counter from listing owner")
                .build();

        GenericDataDTO response = offerService.counterOffer(listingOwnerUser, 55L, counterReq);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.COUNTERED);
        verify(offerRepository, times(2)).save(any(MarketplaceOffer.class));
    }

    @Test
    @DisplayName("accept, reject, and counter should fail when offer status is not PENDING")
    void offerStatusTransitions_failWhenNotPending() {
        MarketplaceOffer acceptedOffer = new MarketplaceOffer();
        acceptedOffer.setId(60L);
        acceptedOffer.setListing(listing);
        acceptedOffer.setBidderCompanyId(200);
        acceptedOffer.setStatus(OfferStatus.ACCEPTED);

        when(offerRepository.findById(60L)).thenReturn(Optional.of(acceptedOffer));

        GenericDataDTO acceptRes = offerService.acceptOffer(listingOwnerUser, 60L);
        assertThat(acceptRes.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);

        GenericDataDTO rejectRes = offerService.rejectOffer(listingOwnerUser, 60L);
        assertThat(rejectRes.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);

        com.cargosetu.marketplace.dto.CounterOfferRequest counterReq = com.cargosetu.marketplace.dto.CounterOfferRequest.builder()
                .amount(BigDecimal.valueOf(40000))
                .build();
        GenericDataDTO counterRes = offerService.counterOffer(listingOwnerUser, 60L, counterReq);
        assertThat(counterRes.getResponseCode()).isEqualTo(CommonStatusCode.FAIL);

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMyOffers returns offers scoped to authenticated company with route enrichment")
    void getMyOffers_returnsEnrichedCompanyOffers() {
        com.cargosetu.marketplace.entity.LoadPost lp = new com.cargosetu.marketplace.entity.LoadPost();
        lp.setOriginCity("Mumbai");
        lp.setOriginState("Maharashtra");
        lp.setDestinationCity("Delhi");
        lp.setDestinationState("Delhi");
        lp.setAskingPrice(BigDecimal.valueOf(50000));
        listing.setLoadPost(lp);

        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(70L);
        offer.setListing(listing);
        offer.setBidderCompanyId(200);
        offer.setOfferedByStaffId(5L);
        offer.setAmount(BigDecimal.valueOf(46000));
        offer.setStatus(OfferStatus.PENDING);

        when(offerRepository.findByBidderCompanyId(200)).thenReturn(java.util.List.of(offer));

        GenericDataDTO res = offerService.getMyOffers(bidderUser);
        assertThat(res.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        assertThat(res.getDataList()).hasSize(1);
        OfferResponse dto = (OfferResponse) res.getDataList().get(0);
        assertThat(dto.getOriginCity()).isEqualTo("Mumbai");
        assertThat(dto.getDestinationCity()).isEqualTo("Delhi");
        assertThat(dto.getListingOwnerCompanyId()).isEqualTo(100);
    }
}
