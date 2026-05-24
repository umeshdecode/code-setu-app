package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "branch")
@Data
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long branchId;

    private String branchName;

    private String branchCode;

    private String contactPerson;

    private String mobile;

    private String email;

    private String gstNumber;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String country;

    private String pincode;

    private Boolean headOffice = false;

    private Boolean active = true;

    private Long companyId;

}
