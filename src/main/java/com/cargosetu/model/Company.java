package com.cargosetu.model;

import com.cargosetu.abstracts.AbstractModel;
import com.cargosetu.dto.CompanyDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "tblmcompany")
@Data
public class Company extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer companyId;

    @NotBlank
    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @NotBlank
    @Email
    @Column(name = "company_email", nullable = false, unique = true, length = 150)
    private String companyEmail;

    @NotNull
    @Column(name = "whatsapp_number", nullable = false)
    private Long whatsappNumber;

    @NotNull
    @Column(name = "contact_no_1", nullable = false)
    private Long contactNo1;

    @Column(name = "contact_no_2")
    private Long contactNo2;

    @Column(name = "landline_no")
    private String landlineNo;

    @Column(name = "toll_free_number")
    private String tollFreeNumber;

    @NotBlank
    @Size(min = 10, max = 10)
    @Column(name = "pan_number", nullable = false, length = 10)
    private String panNumber;

    @NotBlank
    @Size(min = 15, max = 15)
    @Column(name = "gst_number", nullable = false, length = 15)
    private String gstNumber;

    @NotBlank
    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @NotBlank
    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @NotBlank
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @NotBlank
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "branch", length = 100)
    private String branch;

    @Column(name = "main_branch", length = 100)
    private String mainBranch;

    @Column(name = "company_uuid")
    @JsonProperty("company_uuid")
    private String companyUUID;
    @Column(name = "created_by_id")
    private Long createdById;

    public Company() {
    }
    public Company(CompanyDTO companyDto) {
        if (companyDto == null) return;

        this.companyId = companyDto.getCompanyId();
        this.companyName = companyDto.getCompanyName();
        this.companyEmail = companyDto.getCompanyEmail();
        this.whatsappNumber = companyDto.getWhatsappNumber();
        this.contactNo1 = companyDto.getContactNo1();
        this.contactNo2 = companyDto.getContactNo2();
        this.landlineNo = companyDto.getLandlineNo();
        this.tollFreeNumber = companyDto.getTollFreeNumber();
        this.panNumber = companyDto.getPanNumber();
        this.gstNumber = companyDto.getGstNumber();
        this.address = companyDto.getAddress();
        this.country = companyDto.getCountry();
        this.state = companyDto.getState();
        this.city = companyDto.getCity();
        this.branch = companyDto.getBranch();
        this.mainBranch = companyDto.getMainBranch();
    }

}
