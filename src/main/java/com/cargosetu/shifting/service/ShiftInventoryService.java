package com.cargosetu.shifting.service;

import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.shifting.dtos.ShiftInventoryItemDTO;
import com.cargosetu.shifting.entity.ShiftInventoryItem;
import com.cargosetu.shifting.mapper.ShiftInventoryItemMapper;
import com.cargosetu.shifting.repository.ShiftInventoryItemRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class ShiftInventoryService {

    private final ShiftInventoryItemRepository shiftInventoryItemRepository;
    private final ShiftInventoryItemMapper shiftInventoryItemMapper;
    private final ShiftingRepository shiftingRepository;

    public ShiftInventoryService(ShiftInventoryItemRepository shiftInventoryItemRepository,
                                 ShiftInventoryItemMapper shiftInventoryItemMapper,
                                 ShiftingRepository shiftingRepository) {
        this.shiftInventoryItemRepository = shiftInventoryItemRepository;
        this.shiftInventoryItemMapper = shiftInventoryItemMapper;
        this.shiftingRepository = shiftingRepository;
    }

    @Transactional
    public GenericDataDTO createShiftInventoryItem(ShiftInventoryItemDTO itemDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (itemDTO.getShiftId() == null) {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege("Shift ID is required for inventory item.");
                return response;
            }

            Optional<Shifting> shiftingOptional = shiftingRepository.findById(itemDTO.getShiftId());
            if (shiftingOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shifting not found with ID: " + itemDTO.getShiftId());
                return response;
            }

            ShiftInventoryItem item = shiftInventoryItemMapper.toEntity(itemDTO);
            item.setShifting(shiftingOptional.get()); // Link the item to the shifting

            if (item.getRoomName() == null || item.getRoomName().trim().isEmpty()) {
                item.setRoomName("General");
            }
            if (item.getConditionStatus() == null || item.getConditionStatus().trim().isEmpty()) {
                item.setConditionStatus("Good");
            }

            // Set parent references for nested entities
            if (item.getShiftInventoryPhotos() != null) {
                item.getShiftInventoryPhotos().forEach(photo -> photo.setInventoryItem(item));
            }
            if (item.getShiftInventoryPackagings() != null) {
                item.getShiftInventoryPackagings().forEach(pkg -> pkg.setInventoryItem(item));
            }
            if (item.getLoaded() == null) {
                item.setLoaded(false);
            }
            if (item.getUnloaded() == null) {
                item.setUnloaded(false);
            }
            if (item.getPacked() == null) {
                item.setPacked(false);
            }
            ShiftInventoryItem savedItem = shiftInventoryItemRepository.save(item);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Shift inventory item created successfully!");
            response.setData(shiftInventoryItemMapper.toDto(savedItem));

        } catch (Exception e) {
            log.error("Error creating shift inventory item: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while creating the inventory item.");
        }
        return response;
    }

    public GenericDataDTO getShiftInventoryByShiftId(Long shiftId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            java.util.List<ShiftInventoryItem> items = shiftInventoryItemRepository.findByShifting_ShiftingIdOrderByIdAsc(shiftId);
            java.util.List<ShiftInventoryItemDTO> dtos = items.stream()
                    .map(shiftInventoryItemMapper::toDto)
                    .collect(java.util.stream.Collectors.toList());

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shift inventory items retrieved successfully!");
            response.setData(dtos);
        } catch (Exception e) {
            log.error("Error retrieving inventory items for shift ID {}: {}", shiftId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while retrieving inventory items.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO deleteShiftInventoryItem(Long itemId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (!shiftInventoryItemRepository.existsById(itemId)) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shift inventory item not found with ID: " + itemId);
                return response;
            }
            shiftInventoryItemRepository.deleteById(itemId);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shift inventory item deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting shift inventory item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while deleting inventory item.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO createBulkShiftInventoryItems(Long shiftId, java.util.List<ShiftInventoryItemDTO> itemDTOs) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Shifting> shiftingOptional = shiftingRepository.findById(shiftId);
            if (shiftingOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shifting not found with ID: " + shiftId);
                return response;
            }
            Shifting shifting = shiftingOptional.get();

            java.util.List<ShiftInventoryItem> itemsToSave = new java.util.ArrayList<>();
            for (ShiftInventoryItemDTO dto : itemDTOs) {
                dto.setShiftId(shiftId);
                ShiftInventoryItem item = shiftInventoryItemMapper.toEntity(dto);
                item.setShifting(shifting);
                if (item.getRoomName() == null || item.getRoomName().trim().isEmpty()) {
                    item.setRoomName("General");
                }
                if (item.getConditionStatus() == null || item.getConditionStatus().trim().isEmpty()) {
                    item.setConditionStatus("Good");
                }
                if (item.getPacked() == null) item.setPacked(false);
                if (item.getLoaded() == null) item.setLoaded(false);
                if (item.getUnloaded() == null) item.setUnloaded(false);

                itemsToSave.add(item);
            }

            java.util.List<ShiftInventoryItem> savedItems = shiftInventoryItemRepository.saveAll(itemsToSave);
            java.util.List<ShiftInventoryItemDTO> savedDtos = savedItems.stream()
                    .map(shiftInventoryItemMapper::toDto)
                    .collect(java.util.stream.Collectors.toList());

            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Bulk shift inventory items created successfully!");
            response.setData(savedDtos);
        } catch (Exception e) {
            log.error("Error creating bulk shift inventory items for shift ID {}: {}", shiftId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while creating bulk inventory items.");
        }
        return response;
    }

    public GenericDataDTO getShiftInventoryItem(Long itemId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<ShiftInventoryItem> itemOptional = shiftInventoryItemRepository.findById(itemId);
            if (itemOptional.isPresent()) {
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Shift inventory item found successfully!");
                response.setData(shiftInventoryItemMapper.toDto(itemOptional.get()));
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shift inventory item not found with ID: " + itemId);
            }
        } catch (Exception e) {
            log.error("Error getting shift inventory item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while retrieving the inventory item.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO updateShiftInventoryItem(Long itemId, ShiftInventoryItemDTO itemDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<ShiftInventoryItem> existingItemOptional = shiftInventoryItemRepository.findById(itemId);
            if (existingItemOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shift inventory item not found with ID: " + itemId);
                return response;
            }

            ShiftInventoryItem existingItem = existingItemOptional.get();
            // Update fields from DTO to existing entity
            // MapStruct's updateFromDto method (if implemented in BaseUpdateMapper) would be ideal here
            // For now, manual update or rely on toEntity and then copy properties
            // Assuming BaseUpdateMapper has updateFromDto(D dto, @MappingTarget E entity)
            // If not, we'd need to implement it or manually copy properties.
            ShiftInventoryItem updatedItem = shiftInventoryItemMapper.toEntity(itemDTO);
            updatedItem.setId(itemId); // Ensure we update the existing item

            // Re-establish relationships for nested entities if they are being updated
            if (itemDTO.getShiftId() != null && !Objects.equals(existingItem.getShifting().getShiftingId(), itemDTO.getShiftId())) {
                Optional<Shifting> shiftingOptional = shiftingRepository.findById(itemDTO.getShiftId());
                if (shiftingOptional.isEmpty()) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Shifting not found with ID: " + itemDTO.getShiftId());
                    return response;
                }
                updatedItem.setShifting(shiftingOptional.get());
            } else {
                updatedItem.setShifting(existingItem.getShifting()); // Keep existing shifting if not changed
            }

            // Handle nested collections: clear and re-add or use specific update logic
            // For simplicity with CascadeType.ALL + orphanRemoval, we can clear and re-add
            existingItem.getShiftInventoryPhotos().clear();
            if (updatedItem.getShiftInventoryPhotos() != null) {
                updatedItem.getShiftInventoryPhotos().forEach(photo -> photo.setInventoryItem(existingItem));
                existingItem.getShiftInventoryPhotos().addAll(updatedItem.getShiftInventoryPhotos());
            }

            existingItem.getShiftInventoryPackagings().clear();
            if (updatedItem.getShiftInventoryPackagings() != null) {
                updatedItem.getShiftInventoryPackagings().forEach(pkg -> pkg.setInventoryItem(existingItem));
                existingItem.getShiftInventoryPackagings().addAll(updatedItem.getShiftInventoryPackagings());
            }

            // Copy simple properties from updatedItem to existingItem
            existingItem.setRoomName(updatedItem.getRoomName());
            existingItem.setItemName(updatedItem.getItemName());
            existingItem.setCategory(updatedItem.getCategory());
            existingItem.setPackageNumber(updatedItem.getPackageNumber());
            existingItem.setRemarks(updatedItem.getRemarks());
            existingItem.setConditionStatus(updatedItem.getConditionStatus());
            existingItem.setPacked(updatedItem.getPacked());
            existingItem.setLoaded(updatedItem.getLoaded());
            existingItem.setUnloaded(updatedItem.getUnloaded());


            ShiftInventoryItem savedItem = shiftInventoryItemRepository.save(existingItem);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shift inventory item updated successfully!");
            response.setData(shiftInventoryItemMapper.toDto(savedItem));

        } catch (Exception e) {
            log.error("Error updating shift inventory item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the inventory item.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO updateItemStatus(Long itemId, Boolean packed, Boolean loaded, Boolean unloaded) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<ShiftInventoryItem> itemOptional = shiftInventoryItemRepository.findById(itemId);
            if (itemOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shift inventory item not found with ID: " + itemId);
                return response;
            }
            ShiftInventoryItem item = itemOptional.get();
            if (packed != null) item.setPacked(packed);
            if (loaded != null) item.setLoaded(loaded);
            if (unloaded != null) item.setUnloaded(unloaded);

            ShiftInventoryItem updatedItem = shiftInventoryItemRepository.save(item);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shift inventory item status updated successfully!");
            response.setData(shiftInventoryItemMapper.toDto(updatedItem));

        } catch (Exception e) {
            log.error("Error updating status for shift inventory item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the item status.");
        }
        return response;
    }
}
