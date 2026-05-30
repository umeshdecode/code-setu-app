package com.cargosetu.dto;


import lombok.Data;

@Data
public class PermissionDTO {

    private Long permissionId;
    private String code;
    private String name;
    private String description;
    private String module;

}
