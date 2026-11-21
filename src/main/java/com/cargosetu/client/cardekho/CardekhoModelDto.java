package com.cargosetu.client.cardekho;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardekhoModelDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String slug;
    private String url;
    private String videoUrl;
}
