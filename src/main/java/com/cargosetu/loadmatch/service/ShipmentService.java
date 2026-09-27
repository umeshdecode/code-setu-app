package com.cargosetu.loadmatch.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.loadmatch.dtos.ShipmentDTO;
import com.cargosetu.loadmatch.dtos.ShipmentItemDTO;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

@Service
public interface ShipmentService {

    GenericDataDTO createShipment(LoggedInUser user, ShipmentDTO shipmentDTO);

    GenericDataDTO getShipment(Long shipmentId);

    GenericDataDTO getShipments(LoggedInUser user, Integer pageNumber, Integer pageSize);

    GenericDataDTO updateShipment(Long shipmentId, ShipmentDTO shipmentDTO);

    GenericDataDTO deleteShipment(Long shipmentId);

    GenericDataDTO addItem(Long shipmentId, ShipmentItemDTO itemDTO);

    GenericDataDTO updateItem(Long shipmentId, Long itemId, ShipmentItemDTO itemDTO);

    GenericDataDTO deleteItem(Long shipmentId, Long itemId);

    GenericDataDTO searchOpenShipments(String sourceCity, String destinationCity, String vehicleTypeRequired,
                                        Integer pageNumber, Integer pageSize);
}
