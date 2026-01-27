package com.cargosetu.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DepartmentDTO {
    private Integer departmentId;
    private String departmentUuid;
    private String companyUuid;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private Boolean isDeleted;
    private String createdByName;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
