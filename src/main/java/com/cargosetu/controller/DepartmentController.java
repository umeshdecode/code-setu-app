package com.cargosetu.controller;

import com.cargosetu.dto.DepartmentDTO;
import com.cargosetu.service.DepartmentService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
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
    public GenericDataDTO getDepartments() {
        return departmentService.getDepartmentsList();
    }


    @DeleteMapping("/{id}")
    public GenericDataDTO deleteDepartment(@PathVariable Integer id) {
        return departmentService.deleteDepartment(id);
    }


    @PostMapping
    public GenericDataDTO createDepartment(@RequestBody DepartmentDTO departmentDTO) {
        return departmentService.createDepartment(departmentDTO);
    }


    @GetMapping("/{id}")
    public GenericDataDTO getDepartmentById(@PathVariable Integer id) {
        return departmentService.getDepartmentById(id);
    }



    @PutMapping("/{id}")
    public GenericDataDTO updateDepartment(@PathVariable Integer id, @RequestBody DepartmentDTO departmentDTO) {
        return departmentService.updateDepartment(id, departmentDTO);
    }

}
