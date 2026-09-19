package com.cargosetu.loadmatch.service.impl;

import com.cargosetu.loadmatch.dtos.LoadMatchDTO;
import com.cargosetu.loadmatch.dtos.TripShipmentDTO;
import com.cargosetu.loadmatch.entity.LoadMatch;
import com.cargosetu.loadmatch.entity.LoadMatchStatus;
import com.cargosetu.loadmatch.entity.ManifestStatus;
import com.cargosetu.loadmatch.entity.Shipment;
import com.cargosetu.loadmatch.entity.ShipmentStatus;
import com.cargosetu.loadmatch.entity.TripShipment;
import com.cargosetu.loadmatch.mapper.LoadMatchMapper;
import com.cargosetu.loadmatch.mapper.TripShipmentMapper;
import com.cargosetu.loadmatch.repository.LoadMatchRepository;
import com.cargosetu.loadmatch.repository.ShipmentRepository;
import com.cargosetu.loadmatch.repository.TripShipmentRepository;
import com.cargosetu.loadmatch.service.LoadMatchingService;
import com.cargosetu.loadmatch.service.matching.MatchScoringEngine;
import com.cargosetu.model.Trip;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LoadMatchingServiceImpl implements LoadMatchingService {

    // Reasonable default lifetime for a freshly-suggested LoadMatch before it
    // is swept to EXPIRED (Phase 3 concern - the sweep itself isn't built yet).
    private static final int SUGGESTION_EXPIRY_DAYS = 3;

    private static final Set<LoadMatchStatus> NON_TERMINAL_STATUSES = EnumSet.of(LoadMatchStatus.SUGGESTED, LoadMatchStatus.VIEWED);

    private static final Set<ShipmentStatus> CANDIDATE_SHIPMENT_STATUSES = EnumSet.of(
            ShipmentStatus.DRAFT, ShipmentStatus.POSTED, ShipmentStatus.MATCHING, ShipmentStatus.PARTIALLY_ALLOCATED);

    // Phase 3: statuses from which a match can move to REQUESTED.
    private static final Set<LoadMatchStatus> REQUESTABLE_STATUSES = EnumSet.of(LoadMatchStatus.SUGGESTED, LoadMatchStatus.VIEWED);

    // Phase 3: statuses from which a match can move to REJECTED.
    private static final Set<LoadMatchStatus> REJECTABLE_STATUSES = EnumSet.of(
            LoadMatchStatus.SUGGESTED, LoadMatchStatus.VIEWED, LoadMatchStatus.REQUESTED);

    // Phase 3: non-terminal statuses that get swept to EXPIRED once
    // superseded by an acceptance on the same shipment, or once past
    // expiresAt (scheduled sweep).
    private static final Set<LoadMatchStatus> ACTIVE_STATUSES = EnumSet.of(
            LoadMatchStatus.SUGGESTED, LoadMatchStatus.VIEWED, LoadMatchStatus.REQUESTED);

    private final ShipmentRepository shipmentRepository;
    private final TripRepository tripRepository;
    private final LoadMatchRepository loadMatchRepository;
    private final TripShipmentRepository tripShipmentRepository;
    private final LoadMatchMapper loadMatchMapper;
    private final TripShipmentMapper tripShipmentMapper;
    private final MatchScoringEngine matchScoringEngine;

    public LoadMatchingServiceImpl(ShipmentRepository shipmentRepository,
                                    TripRepository tripRepository,
                                    LoadMatchRepository loadMatchRepository,
                                    TripShipmentRepository tripShipmentRepository,
                                    LoadMatchMapper loadMatchMapper,
                                    TripShipmentMapper tripShipmentMapper,
                                    MatchScoringEngine matchScoringEngine) {
        this.shipmentRepository = shipmentRepository;
        this.tripRepository = tripRepository;
        this.loadMatchRepository = loadMatchRepository;
        this.tripShipmentRepository = tripShipmentRepository;
        this.loadMatchMapper = loadMatchMapper;
        this.tripShipmentMapper = tripShipmentMapper;
        this.matchScoringEngine = matchScoringEngine;
    }

    @Override
    @Transactional
    public GenericDataDTO findMatchingTripsForShipment(Long shipmentId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Shipment> shipmentOptional = shipmentRepository.findById(shipmentId);
            if (shipmentOptional.isEmpty() || Boolean.TRUE.equals(shipmentOptional.get().getIsDeleted())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shipment not found with ID: " + shipmentId);
                return response;
            }
            Shipment shipment = shipmentOptional.get();

            // DB-level pre-filter: only trips that could plausibly fit this
            // shipment's declared weight/volume. Null shipment fields simply
            // yield no candidates (comparisons against null bind params match
            // nothing), which is consistent with the scoring engine's
            // fail-closed eligibility gate.
            List<Trip> candidateTrips = tripRepository
                    .findByRemainingWeightKgGreaterThanEqualAndRemainingVolumeCbmGreaterThanEqual(
                            shipment.getTotalWeightKg(), shipment.getTotalVolumeCbm());

            List<LoadMatchDTO> results = new ArrayList<>();
            for (Trip trip : candidateTrips) {
                MatchScoringEngine.MatchScoreResult scoreResult = matchScoringEngine.score(shipment, trip);
                if (!scoreResult.isEligible()) {
                    continue;
                }
                LoadMatch loadMatch = upsertLoadMatch(shipment, trip, scoreResult);
                if (loadMatch != null) {
                    results.add(loadMatchMapper.toDto(loadMatch));
                }
            }
            results.sort(Comparator.comparing(LoadMatchDTO::getOverallScore, Comparator.nullsLast(Comparator.reverseOrder())));

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Matching trips fetched successfully!");
            response.setData(results);
        } catch (Exception e) {
            log.error("Error finding matching trips for shipment ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while finding matching trips.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO findMatchingShipmentsForTrip(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Trip> tripOptional = tripRepository.findById(tripId);
            if (tripOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found with ID: " + tripId);
                return response;
            }
            Trip trip = tripOptional.get();

            // ShipmentRepository (a Phase 1 file) only exposes paged
            // find-all-by-company queries, and is off-limits to modify in
            // this phase, so candidate filtering by status/weight/volume is
            // done in-memory here rather than adding a new query method to it.
            List<Shipment> candidateShipments = shipmentRepository.findAll().stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                    .filter(s -> s.getStatus() != null && CANDIDATE_SHIPMENT_STATUSES.contains(s.getStatus()))
                    .collect(Collectors.toList());

            List<LoadMatchDTO> results = new ArrayList<>();
            for (Shipment shipment : candidateShipments) {
                MatchScoringEngine.MatchScoreResult scoreResult = matchScoringEngine.score(shipment, trip);
                if (!scoreResult.isEligible()) {
                    continue;
                }
                LoadMatch loadMatch = upsertLoadMatch(shipment, trip, scoreResult);
                if (loadMatch != null) {
                    results.add(loadMatchMapper.toDto(loadMatch));
                }
            }
            results.sort(Comparator.comparing(LoadMatchDTO::getOverallScore, Comparator.nullsLast(Comparator.reverseOrder())));

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Matching shipments fetched successfully!");
            response.setData(results);
        } catch (Exception e) {
            log.error("Error finding matching shipments for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while finding matching shipments.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    public GenericDataDTO search(String sourceCity, String destinationCity, Double minAvailableVolume, Double minAvailableWeight) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<LoadMatch> matches = loadMatchRepository.search(sourceCity, destinationCity, minAvailableVolume, minAvailableWeight);
            List<LoadMatchDTO> matchDTOs = matches.stream().map(loadMatchMapper::toDto).collect(Collectors.toList());

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Load matches fetched successfully!");
            response.setData(matchDTOs);
        } catch (Exception e) {
            log.error("Error searching load matches: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while searching load matches.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO markViewed(Long loadMatchId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<LoadMatch> loadMatchOptional = loadMatchRepository.findById(loadMatchId);
            if (loadMatchOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Load match not found with ID: " + loadMatchId);
                return response;
            }
            LoadMatch loadMatch = loadMatchOptional.get();
            // Idempotent: only SUGGESTED -> VIEWED transitions here. Already
            // VIEWED or any terminal status is left untouched rather than
            // erroring, per Phase 2 scope (no REQUESTED/ACCEPTED/REJECTED
            // transitions are implemented yet).
            if (loadMatch.getStatus() == LoadMatchStatus.SUGGESTED) {
                loadMatch.setStatus(LoadMatchStatus.VIEWED);
                loadMatch = loadMatchRepository.save(loadMatch);
            }

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Load match marked as viewed.");
            response.setData(loadMatchMapper.toDto(loadMatch));
        } catch (Exception e) {
            log.error("Error marking load match ID {} as viewed: {}", loadMatchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while marking the load match as viewed.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO requestMatch(Long loadMatchId, Long staffId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<LoadMatch> loadMatchOptional = loadMatchRepository.findById(loadMatchId);
            if (loadMatchOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Load match not found with ID: " + loadMatchId);
                return response;
            }
            LoadMatch loadMatch = loadMatchOptional.get();
            if (!REQUESTABLE_STATUSES.contains(loadMatch.getStatus())) {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege("Load match cannot be requested from its current status: " + loadMatch.getStatus());
                return response;
            }
            loadMatch.setStatus(LoadMatchStatus.REQUESTED);
            loadMatch.setUpdatedById(staffId);
            loadMatch = loadMatchRepository.save(loadMatch);

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Load match requested successfully.");
            response.setData(loadMatchMapper.toDto(loadMatch));
        } catch (Exception e) {
            log.error("Error requesting load match ID {}: {}", loadMatchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while requesting the load match.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO rejectMatch(Long loadMatchId, Long staffId, String reason) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<LoadMatch> loadMatchOptional = loadMatchRepository.findById(loadMatchId);
            if (loadMatchOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Load match not found with ID: " + loadMatchId);
                return response;
            }
            LoadMatch loadMatch = loadMatchOptional.get();
            if (!REJECTABLE_STATUSES.contains(loadMatch.getStatus())) {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege("Load match cannot be rejected from its current status: " + loadMatch.getStatus());
                return response;
            }
            loadMatch.setStatus(LoadMatchStatus.REJECTED);
            loadMatch.setRejectionReason(reason);
            loadMatch.setUpdatedById(staffId);
            loadMatch = loadMatchRepository.save(loadMatch);

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Load match rejected successfully.");
            response.setData(loadMatchMapper.toDto(loadMatch));
        } catch (Exception e) {
            log.error("Error rejecting load match ID {}: {}", loadMatchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while rejecting the load match.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    /**
     * Accept-precondition rule: ONLY a REQUESTED match may be accepted. This
     * forces the two-step request -> accept flow rather than allowing a
     * direct SUGGESTED/VIEWED -> ACCEPTED jump. It is the simplest and
     * safest rule to reason about (one unambiguous predecessor state), it
     * matches the doc's description of a request/accept flow, and it avoids
     * ambiguity about whether a match that nobody explicitly "requested" was
     * really vetted by staff before capacity gets committed.
     */
    @Override
    @Transactional
    public GenericDataDTO acceptMatch(Long loadMatchId, Long staffId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<LoadMatch> loadMatchOptional = loadMatchRepository.findById(loadMatchId);
            if (loadMatchOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Load match not found with ID: " + loadMatchId);
                return response;
            }
            LoadMatch loadMatch = loadMatchOptional.get();
            if (loadMatch.getStatus() != LoadMatchStatus.REQUESTED) {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege("Load match must be in REQUESTED status before it can be accepted. Current status: " + loadMatch.getStatus());
                return response;
            }

            Shipment shipment = loadMatch.getShipment();

            // Re-fetch the Trip fresh inside this transaction rather than
            // reusing the LoadMatch's lazy association - the association may
            // have been loaded earlier (or by a different persistence
            // context) and could be stale relative to concurrent capacity
            // changes made by another staff member's acceptMatch call.
            Optional<Trip> tripOptional = tripRepository.findById(loadMatch.getTrip().getTripId());
            if (tripOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip not found for this load match.");
                return response;
            }
            Trip trip = tripOptional.get();

            if (shipment.getTotalWeightKg() == null || shipment.getTotalVolumeCbm() == null
                    || trip.getRemainingWeightKg() == null || trip.getRemainingVolumeCbm() == null
                    || shipment.getTotalWeightKg() > trip.getRemainingWeightKg()
                    || shipment.getTotalVolumeCbm() > trip.getRemainingVolumeCbm()) {
                response.setResponseCode(CommonStatusCode.CONFLICT);
                response.setResponseMessege("Trip capacity has changed since this match was suggested. Please refresh matches and try again.");
                return response;
            }

            trip.setRemainingWeightKg(trip.getRemainingWeightKg() - shipment.getTotalWeightKg());
            trip.setRemainingVolumeCbm(trip.getRemainingVolumeCbm() - shipment.getTotalVolumeCbm());

            try {
                // saveAndFlush (not save) is required here: the @Version
                // check only happens when Hibernate actually issues the
                // UPDATE ... WHERE version = ? statement, which normally
                // happens lazily at transaction commit. By the time commit
                // runs, this method has already returned and there is no
                // catch block left to handle the failure. Flushing
                // synchronously forces that UPDATE (and therefore the
                // version-mismatch check) to run right here, inside this
                // try/catch, so a concurrent modification is turned into a
                // clean structured response instead of a raw 500 escaping
                // the transaction boundary.
                trip = tripRepository.saveAndFlush(trip);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Optimistic lock conflict accepting load match ID {} onto trip ID {}: {}",
                        loadMatchId, trip.getTripId(), e.getMessage());
                response.setResponseCode(CommonStatusCode.CONFLICT);
                response.setResponseMessege("Trip capacity has changed since this match was suggested. Please refresh matches and try again.");
                return response;
            }

            TripShipment tripShipment = new TripShipment();
            tripShipment.setTrip(trip);
            tripShipment.setShipment(shipment);
            tripShipment.setLoadMatch(loadMatch);
            tripShipment.setAllocatedWeightKg(shipment.getTotalWeightKg());
            tripShipment.setAllocatedVolumeCbm(shipment.getTotalVolumeCbm());
            tripShipment.setManifestStatus(ManifestStatus.CONFIRMED);
            tripShipment.setCompanyId(shipment.getCompanyId() != null ? shipment.getCompanyId() : trip.getCompanyId());
            tripShipment.setIsActive(true);
            tripShipment.setIsDeleted(false);
            tripShipment.setCreatedBy(staffId);
            tripShipment = tripShipmentRepository.save(tripShipment);

            loadMatch.setStatus(LoadMatchStatus.ACCEPTED);
            loadMatch.setUpdatedById(staffId);
            loadMatchRepository.save(loadMatch);

            shipment.setStatus(ShipmentStatus.ALLOCATED);
            shipmentRepository.save(shipment);

            // Any other non-terminal suggestion for this same shipment is now
            // moot - the shipment is fully allocated onto one trip this
            // phase (no splitting), so expire the rest.
            List<LoadMatch> otherActiveMatches = loadMatchRepository
                    .findByShipment_ShipmentIdAndStatusIn(shipment.getShipmentId(), new ArrayList<>(ACTIVE_STATUSES))
                    .stream()
                    .filter(lm -> !lm.getLoadMatchId().equals(loadMatch.getLoadMatchId()))
                    .collect(Collectors.toList());
            for (LoadMatch other : otherActiveMatches) {
                other.setStatus(LoadMatchStatus.EXPIRED);
            }
            if (!otherActiveMatches.isEmpty()) {
                loadMatchRepository.saveAll(otherActiveMatches);
            }

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Load match accepted and shipment allocated to trip.");
            response.setData(tripShipmentMapper.toDto(tripShipment));
        } catch (Exception e) {
            log.error("Error accepting load match ID {}: {}", loadMatchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while accepting the load match.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    /**
     * Scheduled sweep (invoked by LoadMatchExpirySweepJob): moves any
     * SUGGESTED/VIEWED/REQUESTED match whose expiresAt has passed to EXPIRED.
     */
    @Override
    @Transactional
    public void sweepExpiredMatches() {
        List<LoadMatch> expired = loadMatchRepository.findByStatusInAndExpiresAtBefore(
                new ArrayList<>(ACTIVE_STATUSES), LocalDateTime.now());
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(lm -> lm.setStatus(LoadMatchStatus.EXPIRED));
        loadMatchRepository.saveAll(expired);
        log.info("LoadMatch expiry sweep: expired {} match(es).", expired.size());
    }

    /**
     * Creates a new SUGGESTED LoadMatch row for this shipment+trip pair, or
     * refreshes the scores on an existing non-terminal (SUGGESTED/VIEWED) row.
     * If an existing row for the pair is terminal (REQUESTED/ACCEPTED/
     * REJECTED/EXPIRED) it is intentionally left untouched and no duplicate
     * row is created - re-suggesting after rejection/expiry/negotiation is a
     * Phase 3 concern, and silently overwriting an active or decided match
     * here would be surprising. Returns null when skipped for that reason.
     */
    private LoadMatch upsertLoadMatch(Shipment shipment, Trip trip, MatchScoringEngine.MatchScoreResult scoreResult) {
        Optional<LoadMatch> existingOptional = loadMatchRepository
                .findByShipment_ShipmentIdAndTrip_TripId(shipment.getShipmentId(), trip.getTripId());

        LoadMatch loadMatch;
        boolean isNew = existingOptional.isEmpty();
        if (!isNew) {
            LoadMatch existing = existingOptional.get();
            if (!NON_TERMINAL_STATUSES.contains(existing.getStatus())) {
                return null;
            }
            loadMatch = existing;
        } else {
            loadMatch = new LoadMatch();
            loadMatch.setShipment(shipment);
            loadMatch.setTrip(trip);
            loadMatch.setIsActive(true);
            loadMatch.setIsDeleted(false);
            loadMatch.setCompanyId(shipment.getCompanyId() != null ? shipment.getCompanyId() : trip.getCompanyId());
        }

        loadMatch.setOverallScore(scoreResult.getOverallScore());
        loadMatch.setRouteScore(scoreResult.getRouteScore());
        loadMatch.setCapacityScore(scoreResult.getCapacityScore());
        loadMatch.setWeightScore(scoreResult.getWeightScore());
        loadMatch.setTimingScore(scoreResult.getTimingScore());
        loadMatch.setVehicleScore(scoreResult.getVehicleScore());
        loadMatch.setHandlingScore(scoreResult.getHandlingScore());
        // Only new rows start at SUGGESTED - refreshing an existing row's scores
        // (e.g. re-opening the Matching Loads screen) must not silently flip an
        // already-VIEWED match back to looking unread.
        if (isNew) {
            loadMatch.setStatus(LoadMatchStatus.SUGGESTED);
        }
        loadMatch.setExpiresAt(LocalDateTime.now().plusDays(SUGGESTION_EXPIRY_DAYS));

        return loadMatchRepository.save(loadMatch);
    }
}
