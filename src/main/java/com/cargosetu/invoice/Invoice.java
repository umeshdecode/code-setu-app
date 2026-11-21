package com.cargosetu.invoice;

import com.cargosetu.customer.entity.Customer;
import com.cargosetu.model.Company;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "tbl_invoice")
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    private Double subTotal;
    private Double discountAmount;
    private Double taxableAmount;

    private Double cgstAmount;
    private Double sgstAmount;
    private Double igstAmount;

    private Double totalTax;
    private Double grandTotal;

    private String paymentStatus;

    private String notes;

    private Boolean isDeleted = false;
}