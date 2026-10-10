package com.cargosetu.client.trucksfloor;

import com.cargosetu.client.trucksfloor.deserializer.LenientBooleanDeserializer;
import com.cargosetu.client.trucksfloor.deserializer.LenientIntegerDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorTyresDto {

    @JsonDeserialize(using = LenientBooleanDeserializer.class)
    private Boolean tubelessTyres;

    private String frontTyre;
    private String rearTyre;

    @JsonDeserialize(using = LenientIntegerDeserializer.class)
    private Integer numberOfTyres;
}
