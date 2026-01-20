package com.cargosetu.dto;

import lombok.Data;

@Data
public class BaseDocDTO {
    private String eventName;
    private String documentFor;
    private String docType;
    private String filename;
    private String uniqueName;
    private String remarks;
    private Integer mvnoId;
}
