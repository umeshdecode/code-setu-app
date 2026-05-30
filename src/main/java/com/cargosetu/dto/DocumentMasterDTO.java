package com.cargosetu.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentMasterDTO {

    private Long documentId;

    private String code; // AADHAAR, PAN, DL, PASSPORT, RESUME

    private String name; // Aadhaar Card, PAN Card

    private Boolean mandatory;  // true for DRIVER_LICENSE

    private Boolean expiryRequired; // true for license, passport

    private Boolean active = true;
}
