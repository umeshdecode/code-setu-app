package com.cargosetu.controller;

import com.cargosetu.dto.PermissionDTO;
import com.cargosetu.service.PermissionService;

import com.cargosetu.urls.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping(BaseURL.BASE_URL+BaseURL.PERMISSION)
@RestController
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping("")
    public GenericDataDTO getPermission(){
        return permissionService.getPermissionList();
    }


    @DeleteMapping("/{id}")
    public GenericDataDTO deletePermission(@PathVariable Long id) {
        return permissionService.deletePermission(id);
    }


    @PostMapping
    public GenericDataDTO createPermission(@RequestBody PermissionDTO permissionDTO) {
        return permissionService.createPermission(permissionDTO);
    }


    @GetMapping("/{id}")
    public GenericDataDTO getPermissionById(@PathVariable Long id) {
        return permissionService.getPermissionById(id);
    }



    @PutMapping("/{id}")
    public GenericDataDTO updatePermission(@PathVariable Long id, @RequestBody PermissionDTO permissionDTO) {
        return permissionService.updatePermission(id, permissionDTO);
    }



}
