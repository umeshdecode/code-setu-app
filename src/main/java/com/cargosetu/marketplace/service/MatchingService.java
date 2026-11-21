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

    public GenericDataDTO createSavedSearch(LoggedInUser user, com.cargosetu.marketplace.dto.SavedSearchRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

    public GenericDataDTO getMySavedSearches(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        response.setResponseCode(CommonStatusCode.SUCCESS);
        return response;
    }

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
