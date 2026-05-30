package com.cargosetu.customer.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtraChargesDTO {
    private Integer extraChargesId;

    private String chargeName;

    private Double chargeValue;

    private Float chargeRate;
}
