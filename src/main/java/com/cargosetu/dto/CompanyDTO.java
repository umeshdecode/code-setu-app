package com.cargosetu.dto;

import com.cargosetu.model.Company;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyDTO {

    private Integer companyId;

    @NotBlank(message = "Company name is required")
    @Size(max = 150)
    private String companyName;

    @NotBlank(message = "Company email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150)
    private String companyEmail;

    @NotNull(message = "Whatsapp number is required")
    @Digits(integer = 15, fraction = 0)
    private Long whatsappNumber;

    @NotNull(message = "Primary contact number is required")
    @Digits(integer = 15, fraction = 0)
    private Long contactNo1;

    @Digits(integer = 15, fraction = 0)
    private Long contactNo2;

    @Size(max = 20)
    private String landlineNo;

    @Size(max = 20)
    private String tollFreeNumber;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN format")
    private String panNumber;

    @NotBlank(message = "GST number is required")
    @Pattern(
            regexp = "[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}",
            message = "Invalid GST format"
    )
    private String gstNumber;

    @NotBlank(message = "Address is required")
    @Size(max = 300)
    private String address;

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    private String country;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String branch;

    @Size(max = 100)
    private String mainBranch;

    private String companyUUID;
    private String companyNameFirst;
    private String companyNameRest;
    private String logoBase64;
    private String sealBase64;
    private String website;


    public static CompanyDTO from(Company c,
                                  String logoBase64, String sealBase64) {
        String name = c.getCompanyName() != null ? c.getCompanyName().trim() : "";
        int spaceIdx = name.indexOf(' ');
        String first = spaceIdx > 0 ? name.substring(0, spaceIdx) : name;
        String rest  = spaceIdx > 0 ? name.substring(spaceIdx)    : "";

        return CompanyDTO.builder()
                .companyName(name)
                .companyNameFirst(first)
                .companyNameRest(rest)
                .address(c.getAddress())
                .city(c.getCity())
                .state(c.getState())
                .contactNo1(c.getContactNo1())
                .contactNo2(c.getContactNo2())
                .companyEmail(c.getCompanyEmail())
                .whatsappNumber(c.getWhatsappNumber())
                .panNumber(c.getPanNumber())
                .gstNumber(c.getGstNumber())
                .branch(c.getBranch())
                .logoBase64(logoBase64)
                .sealBase64(sealBase64)
                .build();
    }
}

