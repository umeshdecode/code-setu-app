package com.cargosetu.maps.dto;

import lombok.Data;

import java.util.List;

@Data
public class GeocodeCandidateDTO {
    private String formattedAddress;
    private Double latitude;
    private Double longitude;
    private String locationType;
    private List<String> types;
    private boolean partialMatch;
    /**
     * True for city/region-level or approximate results. The UI must warn staff that
     * this is not a precise pickup/drop point before they confirm it.
     */
    private boolean approximate;
    private String placeId;
}
