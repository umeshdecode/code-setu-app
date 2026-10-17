package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.entity.*;
import com.cargosetu.marketplace.enums.*;
import com.cargosetu.marketplace.repository.*;
import com.cargosetu.marketplace.mapper.*;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.repo.DriverRepository;
import com.cargosetu.repo.VehicleRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListingService {

    private final MarketplaceListingRepository listingRepository;
    private final LoadPostRepository loadPostRepository;
    private final TruckPostRepository truckPostRepository;
    private final ListingLocationRepository locationRepository;
    private final VehicleRepository vehicleRepository;
    private final CompanyRepository companyRepository;
    private final DriverRepository driverRepository;
    private final LoadPostMapper loadPostMapper;
    private final TruckPostMapper truckPostMapper;
    private final MarketplaceAuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public GenericDataDTO createLoadPost(LoggedInUser user, CreateLoadPostRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceListing listing = new MarketplaceListing();
            listing.setType(ListingType.LOAD_POST);
            listing.setStatus(ListingStatus.PUBLISHED);
            listing.setValidFrom(LocalDateTime.now());
            listing.setValidUntil(LocalDateTime.now().plusHours(72));
            listing.setCompanyId(user.getCompanyId());
            listing.setPostedByStaffId(user.getStaffId());
            long count = listingRepository.count() + 1;
            listing.setListingNumber(String.format("LD-%s-%04d", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), count));
            listing = listingRepository.save(listing);

            LoadPost loadPost = loadPostMapper.toEntity(request);
            if (!com.cargosetu.marketplace.util.GeoDistanceUtils.isValidCoordinate(loadPost.getOriginLat(), loadPost.getOriginLng())) {
                double[] oCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(null, null, loadPost.getOriginCity());
                if (oCoords != null) {
                    loadPost.setOriginLat(oCoords[0]);
                    loadPost.setOriginLng(oCoords[1]);
                }
            }
            if (!com.cargosetu.marketplace.util.GeoDistanceUtils.isValidCoordinate(loadPost.getDestinationLat(), loadPost.getDestinationLng())) {
                double[] dCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(null, null, loadPost.getDestinationCity());
                if (dCoords != null) {
                    loadPost.setDestinationLat(dCoords[0]);
                    loadPost.setDestinationLng(dCoords[1]);
                }
            }
            loadPost.setListing(listing);
            loadPost = loadPostRepository.save(loadPost);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceListing", listing.getId(), "CREATE_LOAD", null, listing.getStatus().name());

            LoadPostResponse loadResponse = loadPostMapper.toResponseWithListing(loadPost);
            enrichLoadPostResponse(loadResponse, loadPost);
            response.setData(loadResponse);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Load post created successfully");
        } catch (Exception e) {
            log.error("Error creating load post", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to create load post");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO createTruckPost(LoggedInUser user, CreateTruckPostRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (request.getVehicleId() != null && !vehicleRepository.existsById(request.getVehicleId())) {
                var fallbackVehicle = vehicleRepository.findAll().stream().findFirst().orElse(null);
                if (fallbackVehicle != null) {
                    request.setVehicleId(fallbackVehicle.getId());
                }
            }

            MarketplaceListing listing = new MarketplaceListing();
            listing.setType(ListingType.TRUCK_POST);
            listing.setStatus(ListingStatus.PUBLISHED);
            listing.setValidFrom(LocalDateTime.now());
            listing.setValidUntil(LocalDateTime.now().plusHours(72));
            listing.setCompanyId(user.getCompanyId());
            listing.setPostedByStaffId(user.getStaffId());
            long count = listingRepository.count() + 1;
            listing.setListingNumber(String.format("TK-%s-%04d", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), count));
            listing = listingRepository.save(listing);

            TruckPost truckPost = truckPostMapper.toEntity(request);
            if (!com.cargosetu.marketplace.util.GeoDistanceUtils.isValidCoordinate(truckPost.getCurrentLat(), truckPost.getCurrentLng())) {
                double[] cCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(null, null, truckPost.getCurrentCity());
                if (cCoords != null) {
                    truckPost.setCurrentLat(cCoords[0]);
                    truckPost.setCurrentLng(cCoords[1]);
                }
            }
            if (!com.cargosetu.marketplace.util.GeoDistanceUtils.isValidCoordinate(truckPost.getRouteFromLat(), truckPost.getRouteFromLng())
                    && truckPost.getRouteFrom() != null && !truckPost.getRouteFrom().isBlank()) {
                double[] rfCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(null, null, truckPost.getRouteFrom());
                if (rfCoords != null) {
                    truckPost.setRouteFromLat(rfCoords[0]);
                    truckPost.setRouteFromLng(rfCoords[1]);
                }
            }
            if (!com.cargosetu.marketplace.util.GeoDistanceUtils.isValidCoordinate(truckPost.getRouteToLat(), truckPost.getRouteToLng())
                    && truckPost.getRouteTo() != null && !truckPost.getRouteTo().isBlank()) {
                double[] rtCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(null, null, truckPost.getRouteTo());
                if (rtCoords != null) {
                    truckPost.setRouteToLat(rtCoords[0]);
                    truckPost.setRouteToLng(rtCoords[1]);
                }
            }
            truckPost.setListing(listing);
            if(request.getWaypoints() != null) {
                truckPost.setWaypoints(objectMapper.writeValueAsString(request.getWaypoints()));
            }
            if(request.getAcceptedLoadTypes() != null) {
                truckPost.setAcceptedLoadTypes(objectMapper.writeValueAsString(request.getAcceptedLoadTypes()));
            }
            truckPost = truckPostRepository.save(truckPost);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceListing", listing.getId(), "CREATE_TRUCK", null, listing.getStatus().name());

            TruckPostResponse truckResponse = truckPostMapper.toResponse(truckPost);
            enrichTruckPostResponse(truckResponse, truckPost);
            response.setData(truckResponse);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Truck post created successfully");
        } catch (Exception e) {
            log.error("Error creating truck post", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to create truck post");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO publishListing(LoggedInUser user, Long listingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceListing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null || !listing.getCompanyId().equals(user.getCompanyId())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Listing not found or unauthorized");
                return response;
            }
            if (ListingStatus.DRAFT != listing.getStatus()) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Listing is not in DRAFT status");
                return response;
            }

            listing.setStatus(ListingStatus.PUBLISHED);
            listing.setValidFrom(LocalDateTime.now());
            listing.setValidUntil(LocalDateTime.now().plusHours(72)); // 72 hours visibility
            listingRepository.save(listing);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceListing", listingId, "PUBLISH", "DRAFT", "PUBLISHED");

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Listing published successfully");
        } catch (Exception e) {
            log.error("Error publishing listing", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to publish listing");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO closeListing(LoggedInUser user, Long listingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceListing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null || !listing.getCompanyId().equals(user.getCompanyId())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Listing not found or unauthorized");
                return response;
            }

            String oldStatus = listing.getStatus() != null ? listing.getStatus().name() : null;
            listing.setStatus(ListingStatus.CLOSED);
            listingRepository.save(listing);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceListing", listingId, "CLOSE", oldStatus, "CLOSED");

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Listing closed successfully");
        } catch (Exception e) {
            log.error("Error closing listing", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to close listing");
        }
        return response;
    }

    public GenericDataDTO getLoadPost(Long listingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            LoadPost post = loadPostRepository.findByListingId(listingId).orElse(null);
            if(post == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Load post not found");
                return response;
            }
            LoadPostResponse dto = loadPostMapper.toResponseWithListing(post);
            enrichLoadPostResponse(dto, post);
            response.setData(dto);
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            log.error("Error fetching load post", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getTruckPost(Long listingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            TruckPost post = truckPostRepository.findByListingId(listingId).orElse(null);
            if(post == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Truck post not found");
                return response;
            }
            TruckPostResponse dto = truckPostMapper.toResponse(post);
            enrichTruckPostResponse(dto, post);
            response.setData(dto);
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            log.error("Error fetching truck post", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO updateLoadPost(LoggedInUser user, Long listingId, CreateLoadPostRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceListing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null || !listing.getCompanyId().equals(user.getCompanyId())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Listing not found or unauthorized");
                return response;
            }
            if (listing.getStatus() == ListingStatus.BOOKED ||
                listing.getStatus() == ListingStatus.COMPLETED ||
                listing.getStatus() == ListingStatus.CANCELLED ||
                listing.getStatus() == ListingStatus.CLOSED) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Cannot update listing in " + listing.getStatus() + " status");
                return response;
            }

            LoadPost loadPost = loadPostRepository.findByListingId(listingId).orElse(null);
            if (loadPost == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Load post not found");
                return response;
            }

            if (request.getOriginCity() != null) loadPost.setOriginCity(request.getOriginCity());
            if (request.getOriginAddress() != null) loadPost.setOriginAddress(request.getOriginAddress());
            if (request.getOriginState() != null) loadPost.setOriginState(request.getOriginState());
            if (request.getOriginPincode() != null) loadPost.setOriginPincode(request.getOriginPincode());
            if (request.getOriginLat() != null) loadPost.setOriginLat(request.getOriginLat());
            if (request.getOriginLng() != null) loadPost.setOriginLng(request.getOriginLng());

            if (request.getDestinationCity() != null) loadPost.setDestinationCity(request.getDestinationCity());
            if (request.getDestinationAddress() != null) loadPost.setDestinationAddress(request.getDestinationAddress());
            if (request.getDestinationState() != null) loadPost.setDestinationState(request.getDestinationState());
            if (request.getDestinationPincode() != null) loadPost.setDestinationPincode(request.getDestinationPincode());
            if (request.getDestinationLat() != null) loadPost.setDestinationLat(request.getDestinationLat());
            if (request.getDestinationLng() != null) loadPost.setDestinationLng(request.getDestinationLng());

            if (request.getPickupFrom() != null) loadPost.setPickupFrom(request.getPickupFrom());
            if (request.getPickupTo() != null) loadPost.setPickupTo(request.getPickupTo());
            if (request.getDeliveryFrom() != null) loadPost.setDeliveryFrom(request.getDeliveryFrom());
            if (request.getDeliveryTo() != null) loadPost.setDeliveryTo(request.getDeliveryTo());

            if (request.getCargoType() != null) loadPost.setCargoType(request.getCargoType());
            if (request.getCargoDescription() != null) loadPost.setCargoDescription(request.getCargoDescription());
            if (request.getWeight() != null) loadPost.setWeight(request.getWeight());
            if (request.getWeightUnit() != null) loadPost.setWeightUnit(request.getWeightUnit());
            if (request.getVolume() != null) loadPost.setVolume(request.getVolume());
            if (request.getPackageCount() != null) loadPost.setPackageCount(request.getPackageCount());

            if (request.getRequiredTruckType() != null) loadPost.setRequiredTruckType(request.getRequiredTruckType());
            if (request.getRequiredTruckLength() != null) loadPost.setRequiredTruckLength(request.getRequiredTruckLength());
            if (request.getRequiredBodyType() != null) loadPost.setRequiredBodyType(request.getRequiredBodyType());
            if (request.getPriceType() != null) loadPost.setPriceType(request.getPriceType());
            if (request.getAskingPrice() != null) loadPost.setAskingPrice(request.getAskingPrice());
            if (request.getPaymentTerms() != null) loadPost.setPaymentTerms(request.getPaymentTerms());
            if (request.getSpecialInstructions() != null) loadPost.setSpecialInstructions(request.getSpecialInstructions());
            if (request.getDeclaredValue() != null) loadPost.setDeclaredValue(request.getDeclaredValue());

            loadPost = loadPostRepository.save(loadPost);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceListing", listing.getId(), "UPDATE_LOAD", null, listing.getStatus().name());

            LoadPostResponse loadResponse = loadPostMapper.toResponseWithListing(loadPost);
            enrichLoadPostResponse(loadResponse, loadPost);
            response.setData(loadResponse);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Load post updated successfully");
        } catch (Exception e) {
            log.error("Error updating load post", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to update load post: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public GenericDataDTO updateTruckPost(LoggedInUser user, Long listingId, CreateTruckPostRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceListing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null || !listing.getCompanyId().equals(user.getCompanyId())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Listing not found or unauthorized");
                return response;
            }
            if (listing.getStatus() == ListingStatus.BOOKED ||
                listing.getStatus() == ListingStatus.COMPLETED ||
                listing.getStatus() == ListingStatus.CANCELLED ||
                listing.getStatus() == ListingStatus.CLOSED) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Cannot update listing in " + listing.getStatus() + " status");
                return response;
            }

            TruckPost truckPost = truckPostRepository.findByListingId(listingId).orElse(null);
            if (truckPost == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Truck post not found");
                return response;
            }

            if (request.getVehicleId() != null && vehicleRepository.existsById(request.getVehicleId())) {
                truckPost.setVehicleId(request.getVehicleId());
            }
            if (request.getDriverId() != null) truckPost.setDriverId(request.getDriverId());
            if (request.getCurrentCity() != null) truckPost.setCurrentCity(request.getCurrentCity());
            if (request.getCurrentState() != null) truckPost.setCurrentState(request.getCurrentState());
            if (request.getCurrentAddress() != null) truckPost.setCurrentAddress(request.getCurrentAddress());
            if (request.getCurrentLat() != null) truckPost.setCurrentLat(request.getCurrentLat());
            if (request.getCurrentLng() != null) truckPost.setCurrentLng(request.getCurrentLng());
            if (request.getRouteFrom() != null) truckPost.setRouteFrom(request.getRouteFrom());
            if (request.getRouteFromState() != null) truckPost.setRouteFromState(request.getRouteFromState());
            if (request.getRouteFromLat() != null) truckPost.setRouteFromLat(request.getRouteFromLat());
            if (request.getRouteFromLng() != null) truckPost.setRouteFromLng(request.getRouteFromLng());
            if (request.getRouteTo() != null) truckPost.setRouteTo(request.getRouteTo());
            if (request.getRouteToState() != null) truckPost.setRouteToState(request.getRouteToState());
            if (request.getRouteToLat() != null) truckPost.setRouteToLat(request.getRouteToLat());
            if (request.getRouteToLng() != null) truckPost.setRouteToLng(request.getRouteToLng());
            if (request.getWaypoints() != null) {
                truckPost.setWaypoints(objectMapper.writeValueAsString(request.getWaypoints()));
            }
            if (request.getExpectedLocation() != null) truckPost.setExpectedLocation(request.getExpectedLocation());
            if (request.getExpectedEta() != null) truckPost.setExpectedEta(request.getExpectedEta());

            if (request.getTotalCapacity() != null) truckPost.setTotalCapacity(request.getTotalCapacity());
            if (request.getAvailableCapacity() != null) truckPost.setAvailableCapacity(request.getAvailableCapacity());
            if (request.getCapacityUnit() != null) truckPost.setCapacityUnit(request.getCapacityUnit());
            if (request.getAvailableVolume() != null) truckPost.setAvailableVolume(request.getAvailableVolume());
            if (request.getAvailableSpacePercent() != null) truckPost.setAvailableSpacePercent(request.getAvailableSpacePercent());
            if (request.getAcceptedLoadTypes() != null) {
                truckPost.setAcceptedLoadTypes(objectMapper.writeValueAsString(request.getAcceptedLoadTypes()));
            }
            if (request.getTruckStatus() != null) truckPost.setTruckStatus(request.getTruckStatus());

            truckPost = truckPostRepository.save(truckPost);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceListing", listing.getId(), "UPDATE_TRUCK", null, listing.getStatus().name());

            TruckPostResponse truckResponse = truckPostMapper.toResponse(truckPost);
            enrichTruckPostResponse(truckResponse, truckPost);
            response.setData(truckResponse);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Truck post updated successfully");
        } catch (Exception e) {
            log.error("Error updating truck post", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to update truck post: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO searchLoads(LoadPostSearchRequest searchRequest) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (searchRequest == null) {
                searchRequest = new LoadPostSearchRequest();
            }
            final LoadPostSearchRequest req = searchRequest;

            Specification<LoadPost> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                // Join with listing (LEFT JOIN to include unlinked or active listings)
                Join<LoadPost, MarketplaceListing> listingJoin = root.join("listing", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.isNull(listingJoin.get("id")),
                        listingJoin.get("status").isNull(),
                        cb.not(listingJoin.get("status").in(ListingStatus.CLOSED, ListingStatus.CANCELLED, ListingStatus.EXPIRED))
                ));

                if (req.getOriginCity() != null && !req.getOriginCity().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("originCity")), "%" + req.getOriginCity().trim().toLowerCase() + "%"));
                }
                if (req.getDestinationCity() != null && !req.getDestinationCity().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("destinationCity")), "%" + req.getDestinationCity().trim().toLowerCase() + "%"));
                }
                LocalDateTime fromDate = req.getPickupFrom() != null ? req.getPickupFrom() : req.getPickupDate();
                if (fromDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("pickupFrom"), fromDate));
                }
                if (req.getPickupTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("pickupFrom"), req.getPickupTo()));
                }
                if (req.getCargoType() != null && req.getCargoType() != CargoType.ALL) {
                    predicates.add(cb.equal(root.get("cargoType"), req.getCargoType()));
                }
                if (req.getMinWeight() != null && req.getMinWeight() > 0) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("weight"), req.getMinWeight()));
                }
                if (req.getMaxWeight() != null && req.getMaxWeight() > 0) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("weight"), req.getMaxWeight()));
                }
                if (req.getRequiredTruckType() != null && !req.getRequiredTruckType().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("requiredTruckType")), "%" + req.getRequiredTruckType().trim().toLowerCase() + "%"));
                }
                if (req.getRequiredTruckLength() != null) {
                    predicates.add(cb.equal(root.get("requiredTruckLength"), req.getRequiredTruckLength()));
                }
                if (req.getRequiredBodyType() != null && !req.getRequiredBodyType().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("requiredBodyType")), "%" + req.getRequiredBodyType().trim().toLowerCase() + "%"));
                }
                if (req.getMinPrice() != null && req.getMinPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("askingPrice"), req.getMinPrice()));
                }
                if (req.getMaxPrice() != null && req.getMaxPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("askingPrice"), req.getMaxPrice()));
                }
                if (req.getPaymentTerms() != null) {
                    predicates.add(cb.equal(root.get("paymentTerms"), req.getPaymentTerms()));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            int page = (req.getPage() != null && req.getPage() >= 0) ? req.getPage() : 0;
            int size = (req.getSize() != null && req.getSize() > 0) ? req.getSize() : 20;

            Sort sort;
            if (req.getSortBy() != null && !req.getSortBy().isBlank()) {
                Sort.Direction direction = "asc".equalsIgnoreCase(req.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sort = Sort.by(direction, req.getSortBy());
            } else {
                sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
            }

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<LoadPost> pagedResult = loadPostRepository.findAll(spec, pageable);

            List<LoadPostResponse> responseList = pagedResult.getContent().stream().map(post -> {
                LoadPostResponse dto = loadPostMapper.toResponseWithListing(post);
                enrichLoadPostResponse(dto, post);
                return dto;
            }).toList();

            response.setDataList(responseList);
            response.setTotalRecords(pagedResult.getTotalElements());
            response.setPageRecords(pagedResult.getNumberOfElements());
            response.setCurrentPageNumber(pagedResult.getNumber());
            response.setTotalPages(pagedResult.getTotalPages());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Loads fetched successfully");
        } catch (Exception e) {
            log.error("Error searching loads", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to search loads: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO searchTrucks(TruckPostSearchRequest searchRequest) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (searchRequest == null) {
                searchRequest = new TruckPostSearchRequest();
            }
            final TruckPostSearchRequest req = searchRequest;

            String originText = (req.getOriginCity() != null && !req.getOriginCity().isBlank())
                    ? req.getOriginCity().trim()
                    : (req.getCurrentCity() != null && !req.getCurrentCity().isBlank() ? req.getCurrentCity().trim() : null);
            String destText = (req.getDestinationCity() != null && !req.getDestinationCity().isBlank())
                    ? req.getDestinationCity().trim()
                    : (req.getRouteTo() != null && !req.getRouteTo().isBlank() ? req.getRouteTo().trim() : null);

            final double[] originCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(
                    req.getOriginLat(), req.getOriginLng(), originText);
            final double[] destCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(
                    req.getDestinationLat(), req.getDestinationLng(), destText);
            final double radiusKm = com.cargosetu.marketplace.util.GeoDistanceUtils.clampRadiusKm(
                    req.getRadiusKm() != null ? req.getRadiusKm() : req.getMaxDetourKm());
            final boolean useGeoCorridorSearch = (originCoords != null || destCoords != null);

            final com.cargosetu.marketplace.util.GeoDistanceUtils.BoundingBox bbox;
            if (originCoords != null && destCoords != null) {
                bbox = com.cargosetu.marketplace.util.GeoDistanceUtils.computeBoundingBox(
                        originCoords[0], originCoords[1], destCoords[0], destCoords[1], radiusKm);
            } else if (originCoords != null) {
                bbox = com.cargosetu.marketplace.util.GeoDistanceUtils.computeBoundingBox(
                        originCoords[0], originCoords[1], originCoords[0], originCoords[1], radiusKm);
            } else if (destCoords != null) {
                bbox = com.cargosetu.marketplace.util.GeoDistanceUtils.computeBoundingBox(
                        destCoords[0], destCoords[1], destCoords[0], destCoords[1], radiusKm);
            } else {
                bbox = null;
            }

            Specification<TruckPost> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                // Join with listing (LEFT JOIN so unlinked or draft listings are not excluded)
                Join<TruckPost, MarketplaceListing> listingJoin = root.join("listing", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.isNull(listingJoin.get("id")),
                        listingJoin.get("status").isNull(),
                        cb.not(listingJoin.get("status").in(ListingStatus.CLOSED, ListingStatus.CANCELLED, ListingStatus.EXPIRED))
                ));

                // Truck availability status: allow EMPTY_STANDING, EMPTY_MOVING, PARTIALLY_LOADED, FULL
                if (req.getTruckStatus() != null) {
                    predicates.add(cb.equal(root.get("truckStatus"), req.getTruckStatus()));
                } else {
                    predicates.add(cb.or(
                            cb.isNull(root.get("truckStatus")),
                            cb.not(root.get("truckStatus").in(TruckAvailabilityStatus.UNAVAILABLE, TruckAvailabilityStatus.BOOKED))
                    ));
                }

                if (!useGeoCorridorSearch) {
                    if (originText != null && !originText.isBlank()) {
                        String cityPattern = "%" + originText.toLowerCase() + "%";
                        predicates.add(cb.or(
                                cb.like(cb.lower(root.get("currentCity")), cityPattern),
                                cb.like(cb.lower(root.get("routeFrom")), cityPattern)
                        ));
                    }
                    if (destText != null && !destText.isBlank()) {
                        String routePattern = "%" + destText.toLowerCase() + "%";
                        predicates.add(cb.like(cb.lower(root.get("routeTo")), routePattern));
                    }
                } else if (bbox != null) {
                    // Spatial bounding-box prefilter (allowing rows with null coordinates or planned routes to be evaluated via city fallback)
                    Predicate inCurrentBbox = cb.and(
                            cb.isNotNull(root.get("currentLat")),
                            cb.isNotNull(root.get("currentLng")),
                            cb.between(root.get("currentLat"), bbox.minLat(), bbox.maxLat()),
                            cb.between(root.get("currentLng"), bbox.minLng(), bbox.maxLng())
                    );
                    Predicate nullCoordsOrRoute = cb.or(
                            cb.isNull(root.get("currentLat")),
                            cb.isNull(root.get("currentLng")),
                            cb.isNotNull(root.get("routeFrom")),
                            cb.isNotNull(root.get("routeTo"))
                    );
                    predicates.add(cb.or(inCurrentBbox, nullCoordsOrRoute));
                }

                if (req.getMinCapacity() != null && req.getMinCapacity() > 0) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("availableCapacity"), req.getMinCapacity()));
                }
                if (req.getCapacityUnit() != null) {
                    predicates.add(cb.equal(root.get("capacityUnit"), req.getCapacityUnit()));
                }
                if (Boolean.TRUE.equals(req.getVerifiedOnly())) {
                    predicates.add(cb.isTrue(root.get("isVerified")));
                }
                if (req.getMinRating() != null && req.getMinRating() > 0) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), req.getMinRating()));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            int page = (req.getPage() != null && req.getPage() >= 0) ? req.getPage() : 0;
            int size = (req.getSize() != null && req.getSize() > 0) ? req.getSize() : 20;

            Sort sort;
            if (req.getSortBy() != null && !req.getSortBy().isBlank()) {
                Sort.Direction direction = "asc".equalsIgnoreCase(req.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sort = Sort.by(direction, req.getSortBy());
            } else {
                sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
            }

            if (!useGeoCorridorSearch && req.getTruckType() == null && req.getBodyType() == null
                    && req.getTruckLength() == null && req.getCargoType() == null) {
                Pageable pageable = PageRequest.of(page, size, sort);
                Page<TruckPost> pagedResult = truckPostRepository.findAll(spec, pageable);

                List<TruckPostResponse> responseList = pagedResult.getContent().stream().map(post -> {
                    TruckPostResponse dto = truckPostMapper.toResponse(post);
                    enrichTruckPostResponse(dto, post);
                    return dto;
                }).toList();

                response.setDataList(responseList);
                response.setTotalRecords(pagedResult.getTotalElements());
                response.setPageRecords(pagedResult.getNumberOfElements());
                response.setCurrentPageNumber(pagedResult.getNumber());
                response.setTotalPages(pagedResult.getTotalPages());
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Trucks fetched successfully");
                return response;
            }

            // Corridor / enriched attribute filtering with deterministic sorting and pagination
            List<TruckPost> candidates = truckPostRepository.findAll(spec, sort);
            Double searchedRouteDistanceKm = (originCoords != null && destCoords != null)
                    ? com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                            originCoords[0], originCoords[1], destCoords[0], destCoords[1])
                    : null;

            List<TruckPostResponse> matchedList = new ArrayList<>();
            for (TruckPost post : candidates) {
                TruckPostResponse dto = truckPostMapper.toResponse(post);
                enrichTruckPostResponse(dto, post);

                // Additional vehicle/cargo filters
                if (req.getTruckType() != null && !req.getTruckType().isBlank()) {
                    String tt = dto.getTruckType() != null ? dto.getTruckType() : dto.getVehicleType();
                    if (tt == null || !tt.toLowerCase().contains(req.getTruckType().trim().toLowerCase())) {
                        continue;
                    }
                }
                if (req.getBodyType() != null && !req.getBodyType().isBlank()) {
                    if (dto.getBodyType() == null || !dto.getBodyType().toLowerCase().contains(req.getBodyType().trim().toLowerCase())) {
                        continue;
                    }
                }
                if (req.getTruckLength() != null && req.getTruckLength() > 0) {
                    if (dto.getTruckLengthFeet() == null || Math.abs(dto.getTruckLengthFeet() - req.getTruckLength()) > 0.5) {
                        continue;
                    }
                }
                if (req.getCargoType() != null && req.getCargoType() != CargoType.ALL) {
                    if (dto.getAcceptedLoadTypes() != null && !dto.getAcceptedLoadTypes().isEmpty()
                            && !dto.getAcceptedLoadTypes().contains(req.getCargoType())
                            && !dto.getAcceptedLoadTypes().contains(CargoType.ALL)) {
                        continue;
                    }
                }

                if (useGeoCorridorSearch) {
                    if (!matchesCorridorAndPopulateDistances(post, dto, originCoords, destCoords, originText, destText, radiusKm, searchedRouteDistanceKm)) {
                        continue;
                    }
                }
                matchedList.add(dto);
            }

            if (useGeoCorridorSearch && (req.getSortBy() == null || req.getSortBy().isBlank())) {
                matchedList.sort((a, b) -> {
                    double ca = a.getDistanceFromCorridorKm() != null ? a.getDistanceFromCorridorKm() : Double.MAX_VALUE;
                    double cbDist = b.getDistanceFromCorridorKm() != null ? b.getDistanceFromCorridorKm() : Double.MAX_VALUE;
                    int cmp = Double.compare(ca, cbDist);
                    if (cmp != 0) return cmp;
                    double oa = a.getDistanceFromOriginKm() != null ? a.getDistanceFromOriginKm() : Double.MAX_VALUE;
                    double ob = b.getDistanceFromOriginKm() != null ? b.getDistanceFromOriginKm() : Double.MAX_VALUE;
                    cmp = Double.compare(oa, ob);
                    if (cmp != 0) return cmp;
                    if (a.getId() != null && b.getId() != null) {
                        return b.getId().compareTo(a.getId());
                    }
                    return 0;
                });
            }

            int totalElements = matchedList.size();
            int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
            int fromIndex = Math.min(page * size, totalElements);
            int toIndex = Math.min(fromIndex + size, totalElements);
            List<TruckPostResponse> pageSlice = matchedList.subList(fromIndex, toIndex);

            response.setDataList(pageSlice);
            response.setTotalRecords(totalElements);
            response.setPageRecords(pageSlice.size());
            response.setCurrentPageNumber(page);
            response.setTotalPages(totalPages);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trucks fetched successfully");
        } catch (Exception e) {
            log.error("Error searching trucks", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to search trucks: " + e.getMessage());
        }
        return response;
    }

    private boolean matchesCorridorAndPopulateDistances(
            TruckPost post,
            TruckPostResponse dto,
            double[] originCoords,
            double[] destCoords,
            String originText,
            String destText,
            double radiusKm,
            Double searchedRouteDistanceKm) {
        double[] currentCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(
                post.getCurrentLat(), post.getCurrentLng(), post.getCurrentCity());
        double[] routeFromCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(
                post.getRouteFromLat(), post.getRouteFromLng(), post.getRouteFrom());
        double[] routeToCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(
                post.getRouteToLat(), post.getRouteToLng(), post.getRouteTo());

        double[] primaryStartCoords = currentCoords != null ? currentCoords : routeFromCoords;

        // Case 1: Both Origin and Destination coordinates exist -> Corridor Search (default 200 km radius)
        if (originCoords != null && destCoords != null) {
            Double distOrigin = null;
            if (currentCoords != null) {
                distOrigin = com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                        currentCoords[0], currentCoords[1], originCoords[0], originCoords[1]);
            }
            if (routeFromCoords != null) {
                double rfOrigin = com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                        routeFromCoords[0], routeFromCoords[1], originCoords[0], originCoords[1]);
                distOrigin = distOrigin == null ? rfOrigin : Math.min(distOrigin, rfOrigin);
            }

            Double distDest = null;
            if (routeToCoords != null) {
                distDest = com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                        routeToCoords[0], routeToCoords[1], destCoords[0], destCoords[1]);
            } else if (currentCoords != null) {
                distDest = com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                        currentCoords[0], currentCoords[1], destCoords[0], destCoords[1]);
            }

            Double distCorridor = null;
            if (currentCoords != null) {
                distCorridor = com.cargosetu.marketplace.util.GeoDistanceUtils.distanceToSegmentKm(
                        currentCoords[0], currentCoords[1],
                        originCoords[0], originCoords[1],
                        destCoords[0], destCoords[1]);
            }
            if (routeFromCoords != null) {
                double rfCorridor = com.cargosetu.marketplace.util.GeoDistanceUtils.distanceToSegmentKm(
                        routeFromCoords[0], routeFromCoords[1],
                        originCoords[0], originCoords[1],
                        destCoords[0], destCoords[1]);
                distCorridor = distCorridor == null ? rfCorridor : Math.min(distCorridor, rfCorridor);
            }
            if (dto.getWaypoints() != null) {
                for (String wp : dto.getWaypoints()) {
                    double[] wpCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(null, null, wp);
                    if (wpCoords != null) {
                        double wpCorridor = com.cargosetu.marketplace.util.GeoDistanceUtils.distanceToSegmentKm(
                                wpCoords[0], wpCoords[1],
                                originCoords[0], originCoords[1],
                                destCoords[0], destCoords[1]);
                        distCorridor = distCorridor == null ? wpCorridor : Math.min(distCorridor, wpCorridor);
                    }
                }
            }

            if (distCorridor == null) {
                // Fallback to text match if truck has neither coordinates nor recognized city
                return matchesCityText(post, originText, destText);
            }

            if (distCorridor > radiusKm) {
                return false;
            }

            // Directional compatibility check
            if (!com.cargosetu.marketplace.util.GeoDistanceUtils.isDirectionallyCompatible(
                    primaryStartCoords, routeToCoords,
                    originCoords[0], originCoords[1],
                    destCoords[0], destCoords[1],
                    radiusKm)) {
                return false;
            }

            dto.setDistanceFromOriginKm(distOrigin);
            dto.setDistanceFromDestinationKm(distDest);
            dto.setDistanceFromCorridorKm(distCorridor);
            if (dto.getRouteDistanceKm() == null && searchedRouteDistanceKm != null) {
                dto.setRouteDistanceKm(searchedRouteDistanceKm);
            }
            dto.setDistanceType("STRAIGHT_LINE");
            return true;
        }

        // Case 2: Only Origin coordinates exist -> Origin Radius Search (default 200 km)
        if (originCoords != null) {
            Double distOrigin = null;
            if (currentCoords != null) {
                distOrigin = com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                        currentCoords[0], currentCoords[1], originCoords[0], originCoords[1]);
            }
            if (routeFromCoords != null) {
                double rfOrigin = com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                        routeFromCoords[0], routeFromCoords[1], originCoords[0], originCoords[1]);
                distOrigin = distOrigin == null ? rfOrigin : Math.min(distOrigin, rfOrigin);
            }
            if (distOrigin == null) {
                return matchesCityText(post, originText, destText);
            }
            if (distOrigin > radiusKm) {
                return false;
            }
            dto.setDistanceFromOriginKm(distOrigin);
            dto.setDistanceFromCorridorKm(distOrigin);
            dto.setDistanceType("STRAIGHT_LINE");
            return true;
        }

        // Case 3: Only Destination coordinates exist -> Destination Radius Search (default 200 km)
        if (destCoords != null) {
            Double distDest = null;
            if (routeToCoords != null) {
                distDest = com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                        routeToCoords[0], routeToCoords[1], destCoords[0], destCoords[1]);
            }
            if (currentCoords != null) {
                double currDest = com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                        currentCoords[0], currentCoords[1], destCoords[0], destCoords[1]);
                distDest = distDest == null ? currDest : Math.min(distDest, currDest);
            }
            if (distDest == null) {
                return matchesCityText(post, originText, destText);
            }
            if (distDest > radiusKm) {
                return false;
            }
            dto.setDistanceFromDestinationKm(distDest);
            dto.setDistanceFromCorridorKm(distDest);
            dto.setDistanceType("STRAIGHT_LINE");
            return true;
        }

        return true;
    }

    private boolean matchesCityText(TruckPost post, String originText, String destText) {
        if (originText != null && !originText.isBlank()) {
            String needle = originText.trim().toLowerCase();
            boolean matchCurrent = post.getCurrentCity() != null && post.getCurrentCity().toLowerCase().contains(needle);
            boolean matchFrom = post.getRouteFrom() != null && post.getRouteFrom().toLowerCase().contains(needle);
            if (!matchCurrent && !matchFrom) return false;
        }
        if (destText != null && !destText.isBlank()) {
            String needle = destText.trim().toLowerCase();
            boolean matchTo = post.getRouteTo() != null && post.getRouteTo().toLowerCase().contains(needle);
            if (!matchTo) return false;
        }
        return true;
    }

    public GenericDataDTO getMyLoadPosts(LoggedInUser user, String status) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            Specification<LoadPost> spec = (root, query, cb) -> {
                Join<LoadPost, MarketplaceListing> listingJoin = root.join("listing", JoinType.INNER);
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(listingJoin.get("companyId"), user.getCompanyId()));
                if (status != null && !status.isBlank()) {
                    predicates.add(cb.equal(listingJoin.get("status"), ListingStatus.valueOf(status.trim().toUpperCase())));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            List<LoadPost> posts = loadPostRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
            List<LoadPostResponse> dtos = posts.stream().map(post -> {
                LoadPostResponse dto = loadPostMapper.toResponseWithListing(post);
                enrichLoadPostResponse(dto, post);
                return dto;
            }).toList();
            response.setDataList(dtos);
            response.setTotalRecords(dtos.size());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("My load posts retrieved successfully");
        } catch (Exception e) {
            log.error("Error fetching my load posts", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to fetch my load posts: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO getMyTruckPosts(LoggedInUser user, String status) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            Specification<TruckPost> spec = (root, query, cb) -> {
                Join<TruckPost, MarketplaceListing> listingJoin = root.join("listing", JoinType.INNER);
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(listingJoin.get("companyId"), user.getCompanyId()));
                if (status != null && !status.isBlank()) {
                    predicates.add(cb.equal(listingJoin.get("status"), ListingStatus.valueOf(status.trim().toUpperCase())));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            List<TruckPost> posts = truckPostRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
            List<TruckPostResponse> dtos = posts.stream().map(post -> {
                TruckPostResponse dto = truckPostMapper.toResponse(post);
                enrichTruckPostResponse(dto, post);
                return dto;
            }).toList();
            response.setDataList(dtos);
            response.setTotalRecords(dtos.size());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("My truck posts retrieved successfully");
        } catch (Exception e) {
            log.error("Error fetching my truck posts", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to fetch my truck posts: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO getMyListings(LoggedInUser user, String type, String status) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<MarketplaceListing> listings;
            if (type != null && !type.isBlank() && status != null && !status.isBlank()) {
                listings = listingRepository.findByCompanyIdAndTypeAndStatus(
                        user.getCompanyId(),
                        ListingType.valueOf(type),
                        ListingStatus.valueOf(status));
            } else if (type != null && !type.isBlank()) {
                listings = listingRepository.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("companyId"), user.getCompanyId()),
                        cb.equal(root.get("type"), ListingType.valueOf(type))
                ));
            } else if (status != null && !status.isBlank()) {
                listings = listingRepository.findByCompanyIdAndStatus(
                        user.getCompanyId(),
                        ListingStatus.valueOf(status));
            } else {
                listings = listingRepository.findAll((root, query, cb) -> cb.equal(root.get("companyId"), user.getCompanyId()));
            }
            response.setDataList(listings);
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            log.error("Error fetching my listings", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private void enrichLoadPostResponse(LoadPostResponse dto, LoadPost post) {
        if (dto == null || post == null) return;
        if (dto.getListingNumber() == null || dto.getListingNumber().isBlank()) {
            dto.setListingNumber(String.format("LD-%04d", post.getId()));
        }
        if (dto.getStatus() == null) {
            dto.setStatus(ListingStatus.PUBLISHED);
        }
        if (post.getListing() != null && post.getListing().getCompanyId() != null) {
            companyRepository.findById(post.getListing().getCompanyId())
                    .ifPresent(c -> dto.setCompanyName(c.getCompanyName()));
        }
        double[] oCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(
                post.getOriginLat(), post.getOriginLng(), post.getOriginCity());
        double[] dCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(
                post.getDestinationLat(), post.getDestinationLng(), post.getDestinationCity());
        if (oCoords != null) {
            if (dto.getOriginLat() == null) dto.setOriginLat(oCoords[0]);
            if (dto.getOriginLng() == null) dto.setOriginLng(oCoords[1]);
        }
        if (dCoords != null) {
            if (dto.getDestinationLat() == null) dto.setDestinationLat(dCoords[0]);
            if (dto.getDestinationLng() == null) dto.setDestinationLng(dCoords[1]);
        }
        if (oCoords != null && dCoords != null) {
            dto.setRouteDistanceKm(com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                    oCoords[0], oCoords[1], dCoords[0], dCoords[1]));
            dto.setRouteDistanceType("STRAIGHT_LINE");
        }
    }

    private void enrichTruckPostResponse(TruckPostResponse dto, TruckPost post) {
        if (dto == null) return;
        if (dto.getListingNumber() == null || dto.getListingNumber().isBlank()) {
            dto.setListingNumber(String.format("TK-%04d", post.getId()));
        }
        if (dto.getStatus() == null) {
            dto.setStatus(ListingStatus.PUBLISHED);
        }
        if (post.getListing() != null && post.getListing().getCompanyId() != null) {
            companyRepository.findById(post.getListing().getCompanyId())
                    .ifPresent(c -> dto.setCompanyName(c.getCompanyName()));
        } else if (dto.getCompanyId() != null) {
            companyRepository.findById(dto.getCompanyId())
                    .ifPresent(c -> dto.setCompanyName(c.getCompanyName()));
        }
        if (post.getVehicleId() != null) {
            vehicleRepository.findById(post.getVehicleId()).ifPresent(v -> {
                dto.setVehicleNumber(v.getRegistrationNumber());
                dto.setVehicleType(v.getVehicleType());
                if (dto.getTruckType() == null) {
                    dto.setTruckType(v.getTruckType() != null ? v.getTruckType() : v.getVehicleType());
                }
                if (dto.getBodyType() == null) {
                    dto.setBodyType(v.getBodyType());
                }
                if (dto.getTruckLengthFeet() == null) {
                    dto.setTruckLengthFeet(v.getTruckLengthFeet());
                }
                if (dto.getPassingCapacityTons() == null) {
                    dto.setPassingCapacityTons(v.getPassingCapacityTons());
                }
            });
        }
        if (post.getIsVerified() != null && post.getIsVerified()) {
            dto.setVerificationStatus("VERIFIED");
        } else if (dto.getVerificationStatus() == null) {
            dto.setVerificationStatus("UNVERIFIED");
        }
        if (post.getRating() != null) {
            dto.setOverallRating(post.getRating());
        }
        if (post.getDriverId() != null) {
            driverRepository.findById(post.getDriverId()).ifPresent(d -> {
                dto.setDriverName(d.getName());
                dto.setDriverPhone(d.getPhoneNumber());
            });
        }
        if (post.getWaypoints() != null && !post.getWaypoints().isBlank()) {
            try {
                dto.setWaypoints(objectMapper.readValue(post.getWaypoints(), new TypeReference<List<String>>() {}));
            } catch (Exception ignored) {}
        }
        if (post.getAcceptedLoadTypes() != null && !post.getAcceptedLoadTypes().isBlank()) {
            try {
                dto.setAcceptedLoadTypes(objectMapper.readValue(post.getAcceptedLoadTypes(), new TypeReference<List<CargoType>>() {}));
            } catch (Exception ignored) {}
        }

        boolean hasDistinctRouteFrom = post.getRouteFrom() != null
                && !post.getRouteFrom().isBlank()
                && (post.getCurrentCity() == null || !post.getRouteFrom().trim().equalsIgnoreCase(post.getCurrentCity().trim()));
        String startCity = hasDistinctRouteFrom ? post.getRouteFrom() : post.getCurrentCity();
        Double startLat = hasDistinctRouteFrom ? post.getRouteFromLat() : (post.getRouteFromLat() != null ? post.getRouteFromLat() : post.getCurrentLat());
        Double startLng = hasDistinctRouteFrom ? post.getRouteFromLng() : (post.getRouteFromLng() != null ? post.getRouteFromLng() : post.getCurrentLng());
        double[] startCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(startLat, startLng, startCity);
        double[] endCoords = com.cargosetu.marketplace.util.GeoDistanceUtils.resolveCoordinates(
                post.getRouteToLat(), post.getRouteToLng(), post.getRouteTo());
        if (startCoords != null && endCoords != null) {
            dto.setRouteDistanceKm(com.cargosetu.marketplace.util.GeoDistanceUtils.haversineKm(
                    startCoords[0], startCoords[1], endCoords[0], endCoords[1]));
            dto.setDistanceType("STRAIGHT_LINE");
        }
    }
}
