package com.cargosetu.customer.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "tblm_customers")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "firstname")
    private String firstName;

    @Column(name = "lastname")
    private String LastName;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "alternate_mobile_no")
    private String alternateMobileNo;

    @Column(name = "email")
    private String email;

    @Column(name = "work_email")
    private String workEmail;

    @Column(name = "company_id")
    private Integer companyId;
}
