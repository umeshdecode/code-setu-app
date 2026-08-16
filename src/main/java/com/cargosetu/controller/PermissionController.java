package com.cargosetu.controller;

import com.cargosetu.dto.PermissionDTO;
import com.cargosetu.service.PermissionService;

import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping(BaseURL.BASE_URL+BaseURL.PERMISSION)
@RestController
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    @PreAuthorize("@permissionService.hasAccess('PERMISSION', 'READ')")
    public GenericDataDTO getPermission(){
        return permissionService.getPermissionList();
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAccess('PERMISSION', 'DELETE')")
    public GenericDataDTO deletePermission(@PathVariable Long id) {
        return permissionService.deletePermission(id);
    }


    @PostMapping
    @PreAuthorize("@permissionService.hasAccess('PERMISSION', 'CREATE')")
    public GenericDataDTO createPermission(@RequestBody PermissionDTO permissionDTO) {
        return permissionService.createPermission(permissionDTO);
    }


    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAccess('PERMISSION', 'READ')")
    public GenericDataDTO getPermissionById(@PathVariable Long id) {
        return permissionService.getPermissionById(id);
    }



    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasAccess('PERMISSION', 'UPDATE')")
    public GenericDataDTO updatePermission(@PathVariable Long id, @RequestBody PermissionDTO permissionDTO) {
        return permissionService.updatePermission(id, permissionDTO);
    }



}
