package com.cargosetu.catalogue;

import com.cargosetu.dto.VehicleBrandDto;
import com.cargosetu.dto.VehicleCatalogueResponseDto;
import com.cargosetu.dto.VehicleCatalogueSyncResultDto;
import com.cargosetu.dto.VehicleModelDto;
import com.cargosetu.service.VehicleCatalogueService;
import com.cargosetu.service.VehicleCatalogueSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleCatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleCatalogueService catalogueService;

    @MockBean
    private VehicleCatalogueSyncService syncService;

    @Test
    void testGetBrandsPublicAccess() throws Exception {
        when(catalogueService.getBrands()).thenReturn(List.of(
                VehicleBrandDto.builder().id(1L).name("Tata").slug("tata").build(),
                VehicleBrandDto.builder().id(2L).name("Eicher").slug("eicher").build()
        ));

        mockMvc.perform(get("/api/v2/cargosetu/vehicles/brands")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tata"))
                .andExpect(jsonPath("$[0].slug").value("tata"))
                .andExpect(jsonPath("$[1].name").value("Eicher"))
                .andExpect(jsonPath("$[1].slug").value("eicher"));
    }

    @Test
    void testGetModelsByBrandPublicAccess() throws Exception {
        when(catalogueService.getModelsByBrand("tata")).thenReturn(List.of(
                VehicleModelDto.builder().id(1L).name("Intra V30").slug("intra-v30").build()
        ));

        mockMvc.perform(get("/api/v2/cargosetu/vehicles/models")
                        .param("brand", "tata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Intra V30"))
                .andExpect(jsonPath("$[0].slug").value("intra-v30"));
    }

    @Test
    void testGetModelDetailsPublicAccess() throws Exception {
        VehicleCatalogueResponseDto responseDto = VehicleCatalogueResponseDto.builder()
                .id(25L)
                .name("Yodha Crew Cab")
                .slug("yodha-crew-cab")
                .brand(VehicleBrandDto.builder().id(1L).name("Tata").slug("tata").build())
                .build();

        when(catalogueService.getModelDetails(25L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v2/cargosetu/vehicles/models/25")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.name").value("Yodha Crew Cab"))
                .andExpect(jsonPath("$.brand.name").value("Tata"));
    }

    @Test
    void testSearchModelsPublicAccess() throws Exception {
        when(catalogueService.searchModels("intra")).thenReturn(List.of(
                VehicleModelDto.builder().id(1L).name("Intra V30").slug("intra-v30").build()
        ));

        mockMvc.perform(get("/api/v2/cargosetu/vehicles/models/search")
                        .param("keyword", "intra")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Intra V30"));
    }

    @Test
    void testAdminSyncUnauthenticatedIsForbiddenOrUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/vehicle-catalogue/sync/tata"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAdminSyncWithInsufficientRoleIsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/vehicle-catalogue/sync/tata"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERADMIN")
    void testAdminSyncWithSuperAdminRoleSucceeds() throws Exception {
        VehicleCatalogueSyncResultDto resultDto = VehicleCatalogueSyncResultDto.builder()
                .brand("tata")
                .pagesProcessed(13)
                .productsProcessed(181)
                .inserted(20)
                .updated(161)
                .failed(0)
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(syncService.syncBrand("tata")).thenReturn(resultDto);

        mockMvc.perform(post("/api/admin/vehicle-catalogue/sync/tata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("tata"))
                .andExpect(jsonPath("$.pagesProcessed").value(13))
                .andExpect(jsonPath("$.productsProcessed").value(181))
                .andExpect(jsonPath("$.inserted").value(20))
                .andExpect(jsonPath("$.updated").value(161))
                .andExpect(jsonPath("$.failed").value(0));
    }
}
