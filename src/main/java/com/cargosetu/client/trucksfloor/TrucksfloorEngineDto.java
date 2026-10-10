package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.trucksfloor.deserializer.LenientDoubleDeserializer;
import com.cargosetu.client.trucksfloor.deserializer.LenientIntegerDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorEngineDto {

    @JsonDeserialize(using = LenientDoubleDeserializer.class)
    private Double mileage;

    private String engineType;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer engineCylinders;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer engineDisplacement;

    private String engineDisplacementUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer fuelTankCapacity;

    private String fuelTankCapacityUnit;
    private List<String> fuelType;
    private String fuelTypeTxt;

    @JsonDeserialize(using = LenientDoubleDeserializer.class)
    private Double gradeability;

    private String gradeabilityUnit;
    private String emissionNorm;

    @JsonDeserialize(using = LenientDoubleDeserializer.class)
    private Double torque;

    private String torqueUnit;

    @JsonDeserialize(using = LenientDoubleDeserializer.class)
    private Double maxSpeed;

    private String maxSpeedUnit;

    @JsonDeserialize(using = LenientDoubleDeserializer.class)
    private Double enginePower;

    private String enginePowerUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer engineRpm;

    private String engineRpmUnit;
}
