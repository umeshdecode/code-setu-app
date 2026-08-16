package com.cargosetu.controller;

import com.cargosetu.dto.DepartmentDTO;
import com.cargosetu.service.DepartmentService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping(BaseURL.BASE_URL+BaseURL.DEPARTMENT)
@RestController
public class DepartmentController {


    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    private final DepartmentService departmentService;

    @GetMapping("")
    @PreAuthorize("@permissionService.hasAccess('DEPARTMENT', 'READ')")
    public GenericDataDTO getDepartments() {
        return departmentService.getDepartmentsList();
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAccess('DEPARTMENT', 'DELETE')")
    public GenericDataDTO deleteDepartment(@PathVariable Integer id) {
        return departmentService.deleteDepartment(id);
    }


    @PostMapping
    @PreAuthorize("@permissionService.hasAccess('DEPARTMENT', 'CREATE')")
    public GenericDataDTO createDepartment(@RequestBody DepartmentDTO departmentDTO) {
        return departmentService.createDepartment(departmentDTO);
    }


    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAccess('DEPARTMENT', 'READ')")
    public GenericDataDTO getDepartmentById(@PathVariable Integer id) {
        return departmentService.getDepartmentById(id);
    }



    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasAccess('DEPARTMENT', 'UPDATE')")
    public GenericDataDTO updateDepartment(@PathVariable Integer id, @RequestBody DepartmentDTO departmentDTO) {
        return departmentService.updateDepartment(id, departmentDTO);
    }

}
