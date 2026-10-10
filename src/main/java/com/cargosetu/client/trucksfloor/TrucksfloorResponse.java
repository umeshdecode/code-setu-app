package com.cargosetu.client.trucksfloor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrucksfloorResponse {

    private List<TrucksfloorProduct> data;

    @JsonProperty("current_page")
    private Integer currentPage;

    @JsonProperty("last_page")
    private Integer lastPage;

    @JsonProperty("per_page")
    private Integer perPage;

    private Integer total;

    @JsonProperty("first_page_url")
    private String firstPageUrl;

    @JsonProperty("last_page_url")
    private String lastPageUrl;
}
