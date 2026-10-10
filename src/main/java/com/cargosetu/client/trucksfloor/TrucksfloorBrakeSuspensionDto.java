package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.trucksfloor.deserializer.LenientBooleanDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorBrakeSuspensionDto {

    private String axleConfig;
    private String frontAxle;
    private String rearAxle;
    private String frontSuspension;
    private String rearSuspension;
    private String brakeType;

    @JsonDeserialize(using = LenientBooleanDeserializer.class)
    private Boolean abs;
}
