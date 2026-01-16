package com.cargosetu.service;

import com.cargosetu.dto.DepartmentDTO;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

@Service
public interface DepartmentService {

    public GenericDataDTO getDepartmentsList();

    public GenericDataDTO getDepartmentById(Integer id);

    public GenericDataDTO updateDepartment(Integer id, DepartmentDTO pojo);

    public GenericDataDTO deleteDepartment(Integer id);

    public GenericDataDTO createDepartment(DepartmentDTO pojo);

}
