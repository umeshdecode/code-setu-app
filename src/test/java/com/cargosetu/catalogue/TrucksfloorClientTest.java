package com.cargosetu.catalogue;

import com.cargosetu.client.exception.VehicleCatalogueClientException;
import com.cargosetu.client.trucksfloor.TrucksfloorClient;
import com.cargosetu.client.trucksfloor.TrucksfloorClientImpl;
import com.cargosetu.client.trucksfloor.TrucksfloorResponse;
import com.cargosetu.config.TrucksfloorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TrucksfloorClientTest {

    private TrucksfloorClient client;
    private MockRestServiceServer mockServer;
    private TrucksfloorProperties properties;

    @BeforeEach
    void setUp() {
        properties = new TrucksfloorProperties();
        properties.setBaseUrl("https://api.trucksfloor.com");
        properties.setLanguage("en");
        properties.setVcode("trf");
        properties.setMaxRetries(2);

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        client = new TrucksfloorClientImpl(restClient, properties);
    }

    @Test
    void testGetTrucksFirstPageWithHeaders() {
        String jsonResponse = """
                {
                    "current_page": 1,
                    "last_page": 13,
                    "per_page": 15,
                    "total": 181,
                    "first_page_url": "https://api.trucksfloor.com/v1/product/list/truck/tata?page=1",
                    "last_page_url": "https://api.trucksfloor.com/v1/product/list/truck/tata?page=13",
                    "data": [
                        {
                            "id": "1",
                            "vehicleName": "Intra V30",
                            "slug": "intra-v30"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.trucksfloor.com/v1/product/list/truck/tata?page=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "*/*"))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(header("language", "en"))
                .andExpect(header("vcode", "trf"))
                .andExpect(header("Origin", "https://trucksfloor.com"))
                .andExpect(header("Referer", "https://trucksfloor.com/"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        TrucksfloorResponse response = client.getTrucks("tata", 1);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getLastPage()).isEqualTo(13);
        assertThat(response.getTotal()).isEqualTo(181);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getVehicleName()).isEqualTo("Intra V30");
        assertThat(response.getData().get(0).getSlug()).isEqualTo("intra-v30");

        mockServer.verify();
    }

    @Test
    void testGetTrucksSpecificPage() {
        String jsonResponse = """
                {
                    "current_page": 2,
                    "last_page": 13,
                    "per_page": 15,
                    "total": 181,
                    "data": []
                }
                """;

        mockServer.expect(requestTo("https://api.trucksfloor.com/v1/product/list/truck/tata?page=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        TrucksfloorResponse response = client.getTrucks("tata", 2);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentPage()).isEqualTo(2);
        assertThat(response.getData()).isEmpty();

        mockServer.verify();
    }

    @Test
    void testGetTrucksClientErrorThrowsException() {
        mockServer.expect(requestTo("https://api.trucksfloor.com/v1/product/list/truck/unknown?page=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getTrucks("unknown", 1))
                .isInstanceOf(VehicleCatalogueClientException.class)
                .hasMessageContaining("404");

        mockServer.verify();
    }

    @Test
    void testGetTrucksServerErrorRetriesAndFails() {
        mockServer.expect(requestTo("https://api.trucksfloor.com/v1/product/list/truck/tata?page=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        mockServer.expect(requestTo("https://api.trucksfloor.com/v1/product/list/truck/tata?page=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getTrucks("tata", 1))
                .isInstanceOf(VehicleCatalogueClientException.class);

        mockServer.verify();
    }
}
