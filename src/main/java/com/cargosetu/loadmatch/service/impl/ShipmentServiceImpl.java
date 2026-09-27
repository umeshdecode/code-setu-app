package com.cargosetu.loadmatch.service.impl;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.repository.CustomerRepository;
import com.cargosetu.loadmatch.dtos.ShipmentDTO;
import com.cargosetu.loadmatch.dtos.ShipmentItemDTO;
import com.cargosetu.loadmatch.entity.Shipment;
import com.cargosetu.loadmatch.entity.ShipmentItem;
import com.cargosetu.loadmatch.entity.ShipmentStatus;
import com.cargosetu.loadmatch.mapper.ShipmentItemMapper;
import com.cargosetu.loadmatch.mapper.ShipmentMapper;
import com.cargosetu.loadmatch.repository.ShipmentItemRepository;
import com.cargosetu.loadmatch.repository.ShipmentRepository;
import com.cargosetu.loadmatch.service.ShipmentService;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    // Statuses a shipment is considered "open"/searchable for a Find Loads
    // search. DRAFT is intentionally excluded here (unlike LoadMatchingServiceImpl's
    // CANDIDATE_SHIPMENT_STATUSES, which also includes DRAFT for internal
    // matching candidacy): a shipment that hasn't been posted yet shouldn't be
    // publicly discoverable via this search endpoint.
    private static final Set<ShipmentStatus> OPEN_SEARCH_STATUSES = EnumSet.of(
            ShipmentStatus.POSTED, ShipmentStatus.MATCHING, ShipmentStatus.PARTIALLY_ALLOCATED);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentItemRepository shipmentItemRepository;
    private final ShipmentItemMapper shipmentItemMapper;
    private final CustomerRepository customerRepository;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository,
                                ShipmentMapper shipmentMapper,
                                ShipmentItemRepository shipmentItemRepository,
                                ShipmentItemMapper shipmentItemMapper,
                                CustomerRepository customerRepository) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentMapper = shipmentMapper;
        this.shipmentItemRepository = shipmentItemRepository;
        this.shipmentItemMapper = shipmentItemMapper;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public GenericDataDTO createShipment(LoggedInUser user, ShipmentDTO shipmentDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (shipmentDTO.getCustomerId() == null) {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege("Customer ID is required to create a shipment.");
                return response;
            }
            Optional<Customer> customerOptional = customerRepository.findById(shipmentDTO.getCustomerId());
            if (customerOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Customer not found with ID: " + shipmentDTO.getCustomerId());
                return response;
            }

            Shipment shipment = shipmentMapper.toEntity(shipmentDTO);
            shipment.setCustomer(customerOptional.get());
            if (shipment.getStatus() == null) {
                shipment.setStatus(ShipmentStatus.DRAFT);
            }
            if (user != null) {
                shipment.setCompanyId(user.getCompanyId());
                shipment.setCreatedBy(user.getStaffId());
                // LoggedInUser has no getUserName(): Lombok's @Getter skips it because
                // it does a case-insensitive collision check against the manually
                // overridden getUsername() (UserDetails, returns email) - same reason
                // StaffService.getCurrentUserName() builds a display name from
                // first/last name instead of calling a getUserName() that doesn't exist.
                String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";
                String displayName = (firstName + " " + lastName).trim();
                shipment.setCreatedByName(displayName.isEmpty() ? "-" : displayName);
            }
            shipment.setCustId(shipmentDTO.getCustomerId().intValue());
            shipment.setIsActive(true);
            shipment.setIsDeleted(false);

            Shipment savedShipment = shipmentRepository.save(shipment);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Shipment created successfully!");
            response.setData(shipmentMapper.toDto(savedShipment));
        } catch (Exception e) {
            log.error("Error creating shipment: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while creating the shipment.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    public GenericDataDTO getShipment(Long shipmentId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Shipment> shipmentOptional = shipmentRepository.findById(shipmentId);
            if (shipmentOptional.isPresent() && !Boolean.TRUE.equals(shipmentOptional.get().getIsDeleted())) {
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Shipment found successfully!");
                response.setData(shipmentMapper.toDto(shipmentOptional.get()));
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shipment not found with ID: " + shipmentId);
            }
        } catch (Exception e) {
            log.error("Error getting shipment with ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while retrieving the shipment.");
        }
        return response;
    }

    @Override
    public GenericDataDTO getShipments(LoggedInUser user, Integer pageNumber, Integer pageSize) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<Shipment> shipments;
            if (user != null && user.getCompanyId() != null) {
                shipments = shipmentRepository.findByCompanyIdAndIsDeletedOrderByShipmentIdDesc(user.getCompanyId(), false, pageable);
            } else {
                shipments = shipmentRepository.findByIsDeletedOrderByShipmentIdDesc(false, pageable);
            }

            Page<ShipmentDTO> shipmentDTOs = shipments.map(shipmentMapper::toDto);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shipments fetched successfully!");
            response.setData(shipmentDTOs);
        } catch (Exception e) {
            log.error("Error fetching shipments: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while fetching shipments.");
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO updateShipment(Long shipmentId, ShipmentDTO shipmentDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Shipment> existingOptional = shipmentRepository.findById(shipmentId);
            if (existingOptional.isEmpty() || Boolean.TRUE.equals(existingOptional.get().getIsDeleted())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shipment not found with ID: " + shipmentId);
                return response;
            }

            Shipment existingShipment = existingOptional.get();

            if (shipmentDTO.getCustomerId() != null &&
                    (existingShipment.getCustomer() == null ||
                            !shipmentDTO.getCustomerId().equals(existingShipment.getCustomer().getCustomerId()))) {
                Optional<Customer> customerOptional = customerRepository.findById(shipmentDTO.getCustomerId());
                if (customerOptional.isEmpty()) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Customer not found with ID: " + shipmentDTO.getCustomerId());
                    return response;
                }
                existingShipment.setCustomer(customerOptional.get());
                existingShipment.setCustId(shipmentDTO.getCustomerId().intValue());
            }

            shipmentMapper.updateFromDto(shipmentDTO, existingShipment);

            Shipment savedShipment = shipmentRepository.save(existingShipment);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shipment updated successfully!");
            response.setData(shipmentMapper.toDto(savedShipment));
        } catch (Exception e) {
            log.error("Error updating shipment with ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the shipment.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO deleteShipment(Long shipmentId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Shipment> shipmentOptional = shipmentRepository.findById(shipmentId);
            if (shipmentOptional.isPresent()) {
                Shipment shipment = shipmentOptional.get();
                shipment.setIsDeleted(true);
                shipment.setIsActive(false);
                shipmentRepository.save(shipment);
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Shipment deleted successfully!");
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shipment not found with ID: " + shipmentId);
            }
        } catch (Exception e) {
            log.error("Error deleting shipment with ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while deleting the shipment.");
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO addItem(Long shipmentId, ShipmentItemDTO itemDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Shipment> shipmentOptional = shipmentRepository.findById(shipmentId);
            if (shipmentOptional.isEmpty() || Boolean.TRUE.equals(shipmentOptional.get().getIsDeleted())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shipment not found with ID: " + shipmentId);
                return response;
            }

            ShipmentItem item = shipmentItemMapper.toEntity(itemDTO);
            item.setShipment(shipmentOptional.get());

            ShipmentItem savedItem = shipmentItemRepository.save(item);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Shipment item added successfully!");
            response.setData(shipmentItemMapper.toDto(savedItem));
        } catch (Exception e) {
            log.error("Error adding item to shipment ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while adding the shipment item.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO updateItem(Long shipmentId, Long itemId, ShipmentItemDTO itemDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<ShipmentItem> itemOptional = shipmentItemRepository.findById(itemId);
            if (itemOptional.isEmpty() || itemOptional.get().getShipment() == null ||
                    !shipmentId.equals(itemOptional.get().getShipment().getShipmentId())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shipment item not found with ID: " + itemId + " for shipment: " + shipmentId);
                return response;
            }

            ShipmentItem existingItem = itemOptional.get();
            shipmentItemMapper.updateFromDto(itemDTO, existingItem);

            ShipmentItem savedItem = shipmentItemRepository.save(existingItem);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shipment item updated successfully!");
            response.setData(shipmentItemMapper.toDto(savedItem));
        } catch (Exception e) {
            log.error("Error updating shipment item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the shipment item.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Override
    @Transactional
    public GenericDataDTO deleteItem(Long shipmentId, Long itemId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<ShipmentItem> itemOptional = shipmentItemRepository.findById(itemId);
            if (itemOptional.isEmpty() || itemOptional.get().getShipment() == null ||
                    !shipmentId.equals(itemOptional.get().getShipment().getShipmentId())) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shipment item not found with ID: " + itemId + " for shipment: " + shipmentId);
                return response;
            }
            shipmentItemRepository.deleteById(itemId);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shipment item deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting shipment item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while deleting the shipment item.");
        }
        return response;
    }

    @Override
    public GenericDataDTO searchOpenShipments(String sourceCity, String destinationCity, String vehicleTypeRequired,
                                               Integer pageNumber, Integer pageSize) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<Shipment> shipments = shipmentRepository.searchOpen(
                    sourceCity, destinationCity, vehicleTypeRequired,
                    List.copyOf(OPEN_SEARCH_STATUSES), pageable);

            Page<ShipmentDTO> shipmentDTOs = shipments.map(shipmentMapper::toDto);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Open shipments fetched successfully!");
            response.setData(shipmentDTOs);
        } catch (Exception e) {
            log.error("Error searching open shipments: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while searching shipments.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }
}
