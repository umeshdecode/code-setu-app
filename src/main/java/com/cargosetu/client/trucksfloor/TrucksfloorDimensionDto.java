package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.trucksfloor.deserializer.LenientIntegerDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorDimensionDto {

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer turningRadius;
    private String turningRadiusUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer wheelbase;
    private String wheelbaseUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer deckLength;
    private String deckLengthUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer groundClearance;
    private String groundClearanceUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer overallWidth;
    private String overallWidthUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer overallHeight;
    private String overallHeightUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer overallLength;
    private String overallLengthUnit;
}
