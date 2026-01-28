package com.cargosetu.service.impl;

import com.cargosetu.dto.ClientServiceDTO;
import com.cargosetu.mapper.ClientServiceMapper;
import com.cargosetu.model.Clientservice;
import com.cargosetu.repo.ClientServiceRepo;
import com.cargosetu.service.ClientServices;
import com.cargosetu.utils.ApplicationUtils;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class ClientServiceImpl implements ClientServices {
    
    private final ClientServiceRepo clientServiceRepo;
    private final ClientServiceMapper clientServiceMapper;
    
    public ClientServiceImpl(ClientServiceRepo clientServiceRepo, ClientServiceMapper clientServiceMapper) {
        this.clientServiceRepo = clientServiceRepo;
        this.clientServiceMapper = clientServiceMapper;
    }

    @Override
    public GenericDataDTO getClientServiceById(Integer id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching clientServices data with ID: {}", id);

            Optional<Clientservice> data = clientServiceRepo.findById(id);

            if (data.isPresent()) {
                response.setResponseMessege("clientServices data found");
                response.setResponseCode(200);
                response.setData(data.get());
            } else {
                response.setResponseMessege("clientServices data not found");
                response.setResponseCode(404);
                log.info("clientServices data not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.error("Error while fetching clientServices data with ID: {} ",id, e);
            response.setResponseMessege("Failed to fetch clientServices data");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO createClientService(ClientServiceDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {

            boolean serviceDataExistsByCode = clientServiceRepo.existsByNameAndValue(pojo.getName(),pojo.getValue());
            if (serviceDataExistsByCode) {
                response.setResponseMessege("clientServices data already exists, cannot create duplicate!");
                response.setResponseCode(HttpStatus.CONFLICT.value());
                response.setData(null);
                return response;
            }

            log.info("Creating new Client Service data with name: {} ", pojo.getName());

            Clientservice clientservice  = new Clientservice();
            clientservice.setName(pojo.getName());
            clientservice.setValue(pojo.getValue());
            clientservice.setStaffId(pojo.getStaffId());
            clientservice.setModule(pojo.getModule());
            clientservice.setIsActive(true);
            clientservice.setCreatedBy(Long.parseLong(Objects.requireNonNull(ApplicationUtils.getCurrentUser()).getStaffId().toString()));
            clientservice = clientServiceRepo.save(clientservice);

            response.setResponseMessege("Client service data Created Successfully");
            response.setResponseCode(201);
            response.setData(clientservice);

            log.info("Client service data created successfully with ID: {} ", clientservice.getId());
        } catch (Exception e) {
            log.info("Error while creating Client service data ", e);
            response.setResponseMessege("Failed to create Client service data");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO updateClientService(Integer id, ClientServiceDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (pojo == null) {
                response.setResponseMessege("Request body cannot be null");
                response.setResponseCode(400);
                return response;
            }
            log.info("Updating Client service data with ID: {}", id);

            Optional<Clientservice> optionaldata = clientServiceRepo.findById(id);

            if (optionaldata.isPresent()) {
                Clientservice data  = optionaldata.get();
                clientServiceMapper.updateFromDto(pojo, data);
                data.setUpdatedAt(LocalDateTime.now());
                clientServiceRepo.save(data);
                response.setResponseMessege("Client service data updated successfully");
                response.setResponseCode(200);
                response.setData(data);

                log.info("Client service data updated successfully for ID: {}", id);
            } else {
                response.setResponseMessege("Client service data not found");
                response.setResponseCode(404);
                log.info("Update failed. Client service data not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.error("Error while updating Client service data with ID: {}", id, e);
            response.setResponseMessege("Failed to update Client service data");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO getClientServiceList() {
        GenericDataDTO responseDTO = new GenericDataDTO();
        try {
            List<Clientservice> dataList = clientServiceRepo.findAll();
            responseDTO.setResponseMessege("Success");
            responseDTO.setResponseCode(HttpStatus.OK.value());
            responseDTO.setDataList(dataList);
        } catch (Exception e) {
            responseDTO.setResponseMessege("Failed to fetch Client Service data");
        }
        return responseDTO;
    }
}
