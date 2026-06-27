package com.cargosetu.dto;

import com.cargosetu.model.ExpenseType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TripExpenseDTO {
    private Long expenseId;
    private Long tripId;
    private ExpenseType expenseType;
    private Double amount;
    private String description;
    private String billNumber;
    private String receiptUrl;
    private LocalDate expenseDate;
    private Boolean approved;
    private String approvedBy;
}
