package com.cargosetu.controller;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.PlanDTO;
import com.cargosetu.service.PlanService;
import com.cargosetu.utils.BaseURL;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping(BaseURL.BASE_URL+UrlConstants.PLAN)
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).PLAN_CREATE)")
    @PostMapping
    public ApiResponseDto create(@Valid @RequestBody PlanDTO dto) {
        log.info("Received request to create plan");
        return planService.createPlan(dto);
    }
    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).PLAN_UPDATE_DELETE)")
    @PutMapping("/{id}")
    public ApiResponseDto update(
            @PathVariable Integer id,
            @Valid @RequestBody PlanDTO dto) {
        log.info("Received request to update plan, ID: {}", id);
        return planService.updatePlan(id, dto);
    }
    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).PLAN_READ)")
    @GetMapping("/{id}")
    public ApiResponseDto getById(@PathVariable Integer id) {
        log.debug("Received request to fetch plan, ID: {}", id);
        return planService.getPlanById(id);
    }
    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).PLAN_READ)")
    @GetMapping
    public ApiResponseDto getAll() {
        log.debug("Received request to fetch all plans");
        return planService.getAllPlans();
    }
    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).PLAN_UPDATE_DELETE)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        log.warn("Received request to delete plan, ID: {}", id);
        planService.deletePlan(id);
        return ResponseEntity.ok("Plan deleted successfully");
    }
    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).PLAN_READ)")
    @GetMapping("/public")
    public ApiResponseDto getPublicPlan() {
        log.debug("Received request to fetch all plans");
        return planService.getAllPlans();
    }
}

