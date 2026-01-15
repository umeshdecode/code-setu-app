package com.cargosetu.dto;


import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class StaffDTO {
        private Long staffId;
        private String uuid;
        private String userName;
        private String firstName;
        private String lastName;
        private String mobileCode;
        private String mobileNumber;
        private String email;
        private Set<RolesDTO> roles = new HashSet<>();
        private Boolean isActive = false;
}
