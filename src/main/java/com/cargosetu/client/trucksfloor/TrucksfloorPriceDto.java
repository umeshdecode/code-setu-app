package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.trucksfloor.deserializer.LenientBigDecimalDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorPriceDto {

    @JsonDeserialize(using = LenientBigDecimalDeserializer.class)
    private BigDecimal minPrice;

    @JsonDeserialize(using = LenientBigDecimalDeserializer.class)
    private BigDecimal maxPrice;

    private String price;
}
