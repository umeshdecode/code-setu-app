package com.cargosetu.customer.dtos;

import com.cargosetu.customer.entity.ExtraCharges;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChargesDTO {
    private Integer chargeId;

    private Double transportCharge;

    private Double packingCharge;

    private Double loadingCharge;

    private Double unloadingCharge;

    private Double unpackingCharge;

    private Double carTransportCharge;

    private Double riskInsureCharge;

    private Double storageCharge;

    private Double statisticalCharge;

    private Double serviceCharge;

    private Float serviceRate;

    private Float gstRate;

    private Double gstCharge;

    @JsonIgnoreProperties("extracharges")
    private List<ExtraChargesDTO> extraChargesList = new ArrayList<>();
}
