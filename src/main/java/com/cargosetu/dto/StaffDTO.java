package com.cargosetu.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StaffDTO {
    private Long staffId;
    private String uuid;
    private String userName;
    private String firstName;
    private String lastName;
    private String mobileCode;
    private String mobileNumber;
    private String email;
    private String password;
    private Set<RolesDTO> roles = new HashSet<>();
    private Boolean isActive = false;
    private String staffType;
    private String profileImageUrl;
    private String alternateMobile;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String licenseNumber;
    private LocalDate licenseExpiryDate;
    private LocalDateTime lastLoginAt;
    private Integer failedLoginAttempts;
    private LocalDateTime passwordChangedAt;
    private String previousPassword;
    private DepartmentDTO department;
    private Integer companyId;

}
