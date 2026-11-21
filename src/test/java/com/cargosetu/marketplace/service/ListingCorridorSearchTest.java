package com.cargosetu.marketplace.service;

import com.cargosetu.marketplace.dto.TruckPostResponse;
import com.cargosetu.marketplace.dto.TruckPostSearchRequest;
import com.cargosetu.marketplace.entity.MarketplaceListing;
import com.cargosetu.marketplace.entity.TruckPost;
import com.cargosetu.marketplace.enums.ListingStatus;
import com.cargosetu.marketplace.enums.ListingType;
import com.cargosetu.marketplace.enums.TruckAvailabilityStatus;
import com.cargosetu.marketplace.mapper.LoadPostMapper;
import com.cargosetu.marketplace.mapper.TruckPostMapper;
import com.cargosetu.marketplace.repository.ListingLocationRepository;
import com.cargosetu.marketplace.repository.LoadPostRepository;
import com.cargosetu.marketplace.repository.MarketplaceListingRepository;
import com.cargosetu.marketplace.repository.TruckPostRepository;
import com.cargosetu.marketplace.util.GeoDistanceUtils;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingCorridorSearchTest {

    @Mock private MarketplaceListingRepository listingRepository;
    @Mock private LoadPostRepository loadPostRepository;
    @Mock private TruckPostRepository truckPostRepository;
    @Mock private ListingLocationRepository locationRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private LoadPostMapper loadPostMapper;
    @Mock private TruckPostMapper truckPostMapper;
    @Mock private MarketplaceAuditService auditService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ListingService listingService;

    @Test
    @DisplayName("GeoDistanceUtils calculates accurate Haversine and point-to-corridor distances")
    void geoDistanceCalculations_accurate() {
        // Mumbai (19.0760, 72.8777) to Pune (18.5204, 73.8567) is ~120 km straight-line
        double mumbaiPuneKm = GeoDistanceUtils.haversineKm(19.0760, 72.8777, 18.5204, 73.8567);
        assertThat(mumbaiPuneKm).isBetween(115.0, 125.0);

        // Vadodara (22.3072, 73.1812) lies along the Mumbai (19.0760, 72.8777) -> Delhi (28.6139, 77.2090) corridor
        double vadodaraCorridorDist = GeoDistanceUtils.distanceToSegmentKm(
                22.3072, 73.1812,
                19.0760, 72.8777,
                28.6139, 77.2090);
        assertThat(vadodaraCorridorDist).isLessThan(150.0);

        // Chennai (13.0827, 80.2707) is far outside the Mumbai -> Delhi 200 km corridor
        double chennaiCorridorDist = GeoDistanceUtils.distanceToSegmentKm(
                13.0827, 80.2707,
                19.0760, 72.8777,
                28.6139, 77.2090);
        assertThat(chennaiCorridorDist).isGreaterThan(900.0);
    }

    @Test
    @DisplayName("searchTrucks matches trucks near origin and along 200 km corridor while excluding far and opposite-direction trucks")
    @SuppressWarnings("unchecked")
    void searchTrucks_corridorMatchingWithDefault200KmRadius() {
        // Truck 1: Near origin (Thane, ~20 km from Mumbai, standing empty)
        TruckPost nearOriginTruck = buildTruckPost(1L, "Thane", 19.2183, 72.9781, null, null);

        // Truck 2: Intermediate city along Mumbai -> Delhi corridor (Vadodara, heading to Delhi)
        TruckPost corridorTruck = buildTruckPost(2L, "Vadodara", 22.3072, 73.1812, "Surat", "Delhi");

        // Truck 3: Far outside 200 km corridor (Chennai, >1000 km away)
        TruckPost farTruck = buildTruckPost(3L, "Chennai", 13.0827, 80.2707, "Chennai", "Madurai");

        // Truck 4: At Mumbai but committed in opposite direction (heading to Bengaluru)
        TruckPost oppositeTruck = buildTruckPost(4L, "Mumbai", 19.0760, 72.8777, "Mumbai", "Bengaluru");

        when(truckPostRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(nearOriginTruck, corridorTruck, farTruck, oppositeTruck));

        when(truckPostMapper.toResponse(any(TruckPost.class))).thenAnswer(inv -> {
            TruckPost p = inv.getArgument(0);
            TruckPostResponse r = new TruckPostResponse();
            r.setId(p.getId());
            r.setCurrentCity(p.getCurrentCity());
            r.setCurrentLat(p.getCurrentLat());
            r.setCurrentLng(p.getCurrentLng());
            r.setRouteFrom(p.getRouteFrom());
            r.setRouteTo(p.getRouteTo());
            return r;
        });

        TruckPostSearchRequest req = TruckPostSearchRequest.builder()
                .originCity("Mumbai")
                .originLat(19.0760)
                .originLng(72.8777)
                .destinationCity("Delhi")
                .destinationLat(28.6139)
                .destinationLng(77.2090)
                // radiusKm omitted -> defaults to 200 km
                .build();

        GenericDataDTO res = listingService.searchTrucks(req);
        assertThat(res.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);

        List<TruckPostResponse> matches = (List<TruckPostResponse>) res.getDataList();
        assertThat(matches).extracting(TruckPostResponse::getId).containsExactlyInAnyOrder(1L, 2L);

        for (TruckPostResponse m : matches) {
            assertThat(m.getDistanceFromOriginKm()).isNotNull();
            assertThat(m.getDistanceFromCorridorKm()).isNotNull().isLessThanOrEqualTo(200.0);
            assertThat(m.getRouteDistanceKm()).isNotNull().isGreaterThan(900.0);
        }
    }

    private TruckPost buildTruckPost(Long id, String currentCity, Double lat, Double lng, String routeFrom, String routeTo) {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(id);
        listing.setType(ListingType.TRUCK_POST);
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setCompanyId(100 + id.intValue());

        TruckPost tp = new TruckPost();
        tp.setId(id);
        tp.setListing(listing);
        tp.setCurrentCity(currentCity);
        tp.setCurrentLat(lat);
        tp.setCurrentLng(lng);
        tp.setRouteFrom(routeFrom);
        tp.setRouteTo(routeTo);
        tp.setAvailableCapacity(18.0);
        tp.setTotalCapacity(20.0);
        tp.setTruckStatus(TruckAvailabilityStatus.EMPTY_STANDING);
        return tp;
    }
}
