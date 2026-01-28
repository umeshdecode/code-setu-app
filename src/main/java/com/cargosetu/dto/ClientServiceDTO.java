package com.cargosetu.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientServiceDTO {

    private Integer clientId;
    private String value;
    private String module;
    private String name;
    private Integer staffId;
    private Boolean isActive;
    private Boolean isDeleted;
    private String createdByName;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
