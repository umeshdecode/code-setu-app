package com.cargosetu.maps.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoutePointDTO {
    private String city;
    private String address;
    private Double latitude;
    private Double longitude;
    /** True only when staff have confirmed this coordinate. */
    private boolean confirmed;
    private String locationSource;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
}
