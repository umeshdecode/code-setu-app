package com.cargosetu.customer.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiftingDTO {
    private Long shiftingId;

    private boolean isShiftCompleted;

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

    @JsonProperty("charges")
    private ChargesDTO chargesDTO;
}
