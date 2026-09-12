package com.cargosetu.customer.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiftingDTO {
    private Long shiftingId;

    @JsonProperty("isShiftCompleted")
    @JsonAlias({"shiftCompleted", "isShiftCompleted", "is_shift_completed"})
    private boolean isShiftCompleted;

    @JsonProperty("shiftCompleted")
    public boolean getShiftCompletedDuplicate() {
        return isShiftCompleted;
    }

    private String shiftFromCity;

    private String shiftToCity;

    private String toAddress;

    private String fromAddress;

    private String shiftingType;

    private LocalDate shiftingStartDate;

    private LocalDate shiftReachedDate;

    private String shiftCompletedBy;

    private Long shiftCompletedById;

    private Long customerId;
    private Integer companyId;
    private Double tokenMoney;

    @JsonProperty("charges")
    @JsonAlias({"chargesDTO", "chargesDto", "charge", "charges"})
    private ChargesDTO chargesDTO;

    @JsonProperty("chargesDTO")
    public ChargesDTO getChargesDTODuplicate() {
        return chargesDTO;
    }

    private String customerName;
    private String customerMobileNo;
    private String customerEmail;
}
