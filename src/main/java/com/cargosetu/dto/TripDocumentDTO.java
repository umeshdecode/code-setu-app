package com.cargosetu.dto;

import lombok.Data;

@Data
public class TripDocumentDTO {
    private Long id;
    private Long tripId;
    private String documentType;
    private String documentUrl;
    private String fileName;
}
