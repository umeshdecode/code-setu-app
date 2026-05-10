package com.cargosetu.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class QuotationDTO {
    private LocalDate quoteDate;
    private Double    subTotal;
    private Double    grandTotal;
    private Double    tokenMoney;
}
