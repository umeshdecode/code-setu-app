package com.cargosetu.service;

import com.cargosetu.dto.ClientServiceDTO;

import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

@Service
public interface ClientServices {
    GenericDataDTO createClientService(ClientServiceDTO serviceDTO);

    GenericDataDTO updateClientService(Integer id, ClientServiceDTO dto);

    GenericDataDTO getClientServiceById(Integer id);

    GenericDataDTO getClientServiceList();

}
