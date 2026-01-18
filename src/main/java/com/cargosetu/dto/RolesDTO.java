package com.cargosetu.dto;

import lombok.Data;

import java.util.Set;

@Data
public class RolesDTO {
    private Integer roleId;
    private String code;
    private String roleName;
    private String discription;
    private String scope;
    private Set<PermissionDTO> permissions;
}
