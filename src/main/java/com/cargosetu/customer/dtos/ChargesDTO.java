package com.cargosetu.customer.dtos;

import com.cargosetu.customer.entity.ExtraCharges;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChargesDTO {
    private Long chargeId;

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

    @JsonProperty("extracharges")
    @JsonAlias({"extraChargesList", "extraCharges", "extracharges"})
    private List<ExtraChargesDTO> extraChargesList = new ArrayList<>();

    @JsonProperty(value = "extraChargesList", access = JsonProperty.Access.READ_ONLY)
    public List<ExtraChargesDTO> getExtraChargesListDuplicate() {
        return extraChargesList;
    }

    public void setExtraChargesList(List<ExtraChargesDTO> extraChargesList) {
        if (extraChargesList == null) {
            this.extraChargesList = new ArrayList<>();
            return;
        }
        List<ExtraChargesDTO> uniqueList = new ArrayList<>();
        for (ExtraChargesDTO item : extraChargesList) {
            if (item != null) {
                boolean exists = uniqueList.stream().anyMatch(existing ->
                    (item.getExtraChargesId() != null && item.getExtraChargesId().equals(existing.getExtraChargesId())) ||
                    (Objects.equals(item.getChargeName(), existing.getChargeName()) &&
                     Objects.equals(item.getChargeValue(), existing.getChargeValue()))
                );
                if (!exists) {
                    uniqueList.add(item);
                }
            }
        }
        this.extraChargesList = uniqueList;
    }
}
