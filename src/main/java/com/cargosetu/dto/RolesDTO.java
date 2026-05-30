package com.cargosetu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RolesDTO {
    private Long roleId;
    private String code;
    private String roleName;
    private String description;
    private String scope;
    private Set<PermissionDTO> permissions;
    private Set<Long> permissionIds;
}
