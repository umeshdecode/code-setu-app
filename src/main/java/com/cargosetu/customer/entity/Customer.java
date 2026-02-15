package com.cargosetu.customer.entity;

import com.cargosetu.document.model.DocumentUpload;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private String lastName;

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

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "is_delete")
    private Boolean isDelete  = false;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    private List<Shifting> shiftings = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    private List<DocumentUpload> documentUploads = new ArrayList<>();
}
