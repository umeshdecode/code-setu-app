package com.cargosetu.controller;

import com.cargosetu.dto.RolesDTO;
import com.cargosetu.service.impl.RolesImpl;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping(BaseURL.BASE_URL+BaseURL.ROLES)
@RestController
public class RoleController {
    @Autowired
    private RolesImpl roleService ;

    @GetMapping
    @PreAuthorize("@permissionService.hasAccess('ROLE', 'READ')")
    public GenericDataDTO getRoles(){
       return roleService.getRolseList();
    }
    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAccess('ROLE', 'READ')")
    public GenericDataDTO getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    @PreAuthorize("@permissionService.hasAccess('ROLE', 'CREATE')")
    @PostMapping
    public GenericDataDTO createRole(@RequestBody RolesDTO role) {
        return roleService.createRole(role);
    }
    // UPDATE ROLE
    @PutMapping
    @PreAuthorize("@permissionService.hasAccess('ROLE', 'UPDATE')")
    public GenericDataDTO updateRole(@RequestBody RolesDTO role) {
        return roleService.updateRole( role);
    }

    // DELETE ROLE
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAccess('ROLE', 'DELETE')")
    public GenericDataDTO deleteRole(@PathVariable Long id) {
        return roleService.deleteRole(id);
    }

}
