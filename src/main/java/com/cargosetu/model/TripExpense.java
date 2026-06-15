package com.cargosetu.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "tbl_trip_expense")
@Data
public class TripExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long expenseId;

    @ManyToOne
    private Trip trip;

    @Enumerated(EnumType.STRING)
    private ExpenseType expenseType;

    private Double amount;

    private String description;

    private String billNumber;

    private String receiptUrl;

    private LocalDate expenseDate;
}