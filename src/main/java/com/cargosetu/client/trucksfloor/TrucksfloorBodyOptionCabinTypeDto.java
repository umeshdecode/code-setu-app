package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.trucksfloor.deserializer.LenientIntegerDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorBodyOptionCabinTypeDto {

    private String chassisType;
    private String cabinType;
    private String bodyOption;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer seatingCapacity;
}
