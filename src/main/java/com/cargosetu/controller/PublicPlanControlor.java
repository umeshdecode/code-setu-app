package com.cargosetu.controller;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.service.PlanService;
import com.cargosetu.utils.BaseURL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping(BaseURL.PUBLIC_PLAN+ UrlConstants.PLAN)
@RequiredArgsConstructor
public class PublicPlanControlor {
    private final PlanService planService;

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).PLAN_READ)")
    @GetMapping("/public")
    public ApiResponseDto getPublicPlan() {
        log.debug("Received request to fetch all plans");
        return planService.getAllPlans();
    }
}
