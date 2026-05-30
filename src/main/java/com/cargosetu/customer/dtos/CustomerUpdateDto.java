package com.cargosetu.customer.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerUpdateDto {
    private Long customerId;
    private String firstName;
    private String lastName;
    private String mobileNo;
    private String alternateMobileNo;
    private String email;
    private String workEmail;
    private String uuid;

}
