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

    @GetMapping("")
    public GenericDataDTO getRoles(){
       return roleService.getRolseList();
    }
    @GetMapping("/{id}")
    public GenericDataDTO getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).STAFF_CREATE)"
    )
    @PostMapping
    public GenericDataDTO createRole(@RequestBody RolesDTO role) {
        return roleService.createRole(role);
    }
    // UPDATE ROLE
    @PutMapping("/{id}")
    public GenericDataDTO updateRole(@PathVariable Long id, @RequestBody RolesDTO role) {
        return roleService.updateRole(id, role);
    }

    // DELETE ROLE
    @DeleteMapping("/{id}")
    public GenericDataDTO deleteRole(@PathVariable Long id) {
        return roleService.deleteRole(id);
    }

}
