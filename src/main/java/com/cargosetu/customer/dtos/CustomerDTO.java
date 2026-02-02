package com.cargosetu.customer.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

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
    private boolean isShiftCompleted;
    private String shiftFromCity;
    private String shiftToCity;
    private String toAddress;
    private String fromAddress;
    private String shiftingType;
    private LocalDate shiftingStartDate;
    private LocalDate shiftReachedDate;
    private Integer companyId;

}
