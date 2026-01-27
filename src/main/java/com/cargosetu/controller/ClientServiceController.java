package com.cargosetu.controller;

import com.cargosetu.dto.ClientServiceDTO;
import com.cargosetu.service.ClientServices;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL+BaseURL.SYSTEM_CONFIG)
public class ClientServiceController {

    public ClientServiceController(ClientServices clientServices) {
        this.clientServices = clientServices;
    }

    private final ClientServices clientServices;

    @GetMapping("")
    public GenericDataDTO getClientService() {
        return clientServices.getClientServiceList();
    }
    

    @PostMapping
    public GenericDataDTO createClientService(@RequestBody ClientServiceDTO clientServiceDTO) {
        return clientServices.createClientService(clientServiceDTO);
    }


    @GetMapping("/{id}")
    public GenericDataDTO getClientServiceById(@PathVariable Integer id) {
        return clientServices.getClientServiceById(id);
    }



    @PutMapping("/{id}")
    public GenericDataDTO updateClientService(@PathVariable Integer id, @RequestBody ClientServiceDTO clientServiceDTO) {
        return clientServices.updateClientService(id, clientServiceDTO);
    }
    
}
