package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.trucksfloor.deserializer.LenientIntegerDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorTransmissionLoadingDto {

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer kerbWeight;
    private String kerbWeightUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer gvwGcw;
    private String gvwGcwUnit;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer payload;
    private String payloadUnit;

    private String gearbox;
}
