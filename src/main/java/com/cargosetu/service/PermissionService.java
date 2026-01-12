package com.cargosetu.service;

import com.cargosetu.dto.PermissionDTO;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

@Service
public interface PermissionService {


    public GenericDataDTO  getPermissionList();

    public GenericDataDTO getPermissionById(Long id);

    public GenericDataDTO updatePermission(Long id, PermissionDTO pojo);

    public GenericDataDTO deletePermission(Long id);

    public GenericDataDTO createPermission(PermissionDTO pojo);
}
