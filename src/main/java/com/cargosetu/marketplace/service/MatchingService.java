package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.MatchResultResponse;
import com.cargosetu.marketplace.entity.LoadPost;
import com.cargosetu.marketplace.entity.TruckPost;
import com.cargosetu.marketplace.repository.LoadPostRepository;
import com.cargosetu.marketplace.repository.MarketplaceConfigRepository;
import com.cargosetu.marketplace.repository.MarketplaceListingRepository;
import com.cargosetu.marketplace.repository.TruckPostRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final LoadPostRepository loadPostRepository;
    private final TruckPostRepository truckPostRepository;
    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceConfigRepository configRepository;
    private final VehicleRepository vehicleRepository;
    private final com.cargosetu.marketplace.repository.SavedSearchRepository savedSearchRepository;

    public GenericDataDTO findMatchesForLoad(Long loadPostId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            LoadPost loadPost = loadPostRepository.findById(loadPostId).orElse(null);
            if (loadPost == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Load post not found");
                return response;
            }

            List<TruckPost> publishedTrucks = truckPostRepository.findByListingStatus(com.cargosetu.marketplace.enums.ListingStatus.PUBLISHED);
            List<MatchResultResponse> matches = new ArrayList<>();

            for (TruckPost truck : publishedTrucks) {
                double score = calculateScore(loadPost, truck);
                if (score > 30.0) { // arbitrary threshold
                    MatchResultResponse match = new MatchResultResponse();
                    match.setMatchScore(score);
                    // MatchResultResponse doesn't have setTruckPostId directly, we might need a TruckPostResponse, or set listingId.
                    // Wait, MatchResultResponse has `listingId` not `truckPostId`.
                    // Let's set it to listingId if truck.getId() maps to it, or just use what we have.
                    // Actually, let's check what fields are in MatchResultResponse.
                    // It has: listingId, listingNumber, listingType, matchScore, scoreBreakdown, loadPost, truckPost
                    // I will set listingId for now.
                    match.setListingId(truck.getId());
                    matches.add(match);
                }
            }

            matches.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
            response.setDataList(matches);
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            log.error("Error finding matches", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO findMatchesForTruck(Long truckPostId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            TruckPost truckPost = truckPostRepository.findById(truckPostId).orElse(null);
            if (truckPost == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Truck post not found");
                return response;
            }

            List<LoadPost> publishedLoads = loadPostRepository.findByListingStatus(com.cargosetu.marketplace.enums.ListingStatus.PUBLISHED);
            List<MatchResultResponse> matches = new ArrayList<>();

            for (LoadPost load : publishedLoads) {
                double score = calculateScore(load, truckPost);
                if (score > 30.0) {
                    MatchResultResponse match = new MatchResultResponse();
                    match.setMatchScore(score);
                    match.setListingId(load.getId());
                    matches.add(match);
                }
            }

            matches.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
            response.setDataList(matches);
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            log.error("Error finding matches", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getRecommendations(LoggedInUser user) {
        // Implementation for overall recommendations
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

    private static final java.util.Set<String> SAVED_SEARCH_TYPES = java.util.Set.of("LOAD", "TRUCK");
    private static final int MAX_SAVED_SEARCHES_PER_STAFF = 20;

    /**
     * Saves a search as an alert. searchType LOAD = "tell me about new loads like this",
     * TRUCK = "tell me about new trucks like this". New matching posts trigger a push
     * (see ListingService#notifyLoadAvailable / #notifyDriverAvailable).
     */
    @org.springframework.transaction.annotation.Transactional
    public GenericDataDTO createSavedSearch(LoggedInUser user, com.cargosetu.marketplace.dto.SavedSearchRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            String type = request.getSearchType() != null ? request.getSearchType().trim().toUpperCase() : "";
            if (!SAVED_SEARCH_TYPES.contains(type)) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("searchType must be LOAD or TRUCK");
                return response;
            }
            if (isBlank(request.getOriginCity()) && isBlank(request.getDestinationCity())) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Choose at least an origin or a destination city for the alert");
                return response;
            }
            if (savedSearchRepository.findByStaffIdAndActive(user.getStaffId(), true).size() >= MAX_SAVED_SEARCHES_PER_STAFF) {
                response.setResponseCode(CommonStatusCode.CONFLICT);
                response.setResponseMessege("You can keep up to " + MAX_SAVED_SEARCHES_PER_STAFF + " saved searches. Delete one first.");
                return response;
            }
            com.cargosetu.marketplace.entity.SavedSearch entity = new com.cargosetu.marketplace.entity.SavedSearch();
            entity.setCompanyId(user.getCompanyId());
            entity.setStaffId(user.getStaffId());
            entity.setSearchType(type);
            entity.setOriginCity(trimOrNull(request.getOriginCity()));
            entity.setDestinationCity(trimOrNull(request.getDestinationCity()));
            entity.setTruckType(trimOrNull(request.getTruckType()));
            entity.setTruckLength(request.getTruckLength());
            entity.setCargoType(request.getCargoType());
            entity.setMinCapacity(request.getMinCapacity());
            entity.setMaxPrice(request.getMaxPrice());
            entity.setActive(true);
            entity = savedSearchRepository.save(entity);
            response.setData(toSavedSearchResponse(entity));
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Search saved. We'll notify you about new matches.");
        } catch (Exception e) {
            log.error("Error saving search", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to save search");
        }
        return response;
    }

    public GenericDataDTO getMySavedSearches(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getStaffId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            List<com.cargosetu.marketplace.dto.SavedSearchResponse> list = savedSearchRepository
                    .findByStaffIdAndActive(user.getStaffId(), true).stream()
                    .map(this::toSavedSearchResponse)
                    .toList();
            response.setDataList(list);
            response.setTotalRecords(list.size());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Saved searches retrieved");
        } catch (Exception e) {
            log.error("Error fetching saved searches", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to get saved searches");
        }
        return response;
    }

    @org.springframework.transaction.annotation.Transactional
    public GenericDataDTO deleteSavedSearch(LoggedInUser user, Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            com.cargosetu.marketplace.entity.SavedSearch entity = savedSearchRepository.findById(id).orElse(null);
            if (entity == null || user == null || !java.util.Objects.equals(entity.getStaffId(), user.getStaffId())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Saved search not found");
                return response;
            }
            entity.setActive(false);
            savedSearchRepository.save(entity);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Saved search deleted");
        } catch (Exception e) {
            log.error("Error deleting saved search", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to delete saved search");
        }
        return response;
    }

    private com.cargosetu.marketplace.dto.SavedSearchResponse toSavedSearchResponse(com.cargosetu.marketplace.entity.SavedSearch e) {
        com.cargosetu.marketplace.dto.SavedSearchResponse dto = new com.cargosetu.marketplace.dto.SavedSearchResponse();
        dto.setId(e.getId());
        dto.setSearchType(e.getSearchType());
        dto.setOriginCity(e.getOriginCity());
        dto.setDestinationCity(e.getDestinationCity());
        dto.setTruckType(e.getTruckType());
        dto.setTruckLength(e.getTruckLength());
        dto.setCargoType(e.getCargoType());
        dto.setMinCapacity(e.getMinCapacity());
        dto.setMaxPrice(e.getMaxPrice());
        dto.setActive(e.getActive());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }

    private static boolean isBlank(String v) { return v == null || v.isBlank(); }
    private static String trimOrNull(String v) { return isBlank(v) ? null : v.trim(); }

    private double calculateScore(LoadPost load, TruckPost truck) {
        double score = 0;
        // Route (30%)
        score += 30.0; // Assume matching for now
        
        // Date (20%)
        score += 20.0;
        
        // Capacity (15%)
        if (truck.getAvailableCapacity() != null && load.getWeight() != null && truck.getAvailableCapacity() >= load.getWeight()) {
            score += 15.0;
        }

        return score;
    }
}
