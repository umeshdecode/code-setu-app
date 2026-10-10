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
}
