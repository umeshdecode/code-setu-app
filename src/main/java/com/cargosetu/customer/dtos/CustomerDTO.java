package com.cargosetu.customer.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDTO {
    private Long customerId;
    private String firstName;
    private String lastName;
    private String mobileNo;
    private String alternateMobileNo;
    private String email;
    private String workEmail;

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
    private Integer companyId;
    private String uuid;
    private Boolean isDelete;

    @JsonProperty("shiftings")
    List<ShiftingDTO> shiftings = new ArrayList<>();


}
