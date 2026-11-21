package com.cargosetu.service;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.dto.PlanDTO;

import java.util.List;

public interface PlanService {

    ApiResponseDto createPlan(PlanDTO planDTO);

    ApiResponseDto updatePlan(Integer planId, PlanDTO planDTO);

    ApiResponseDto getPlanById(Integer planId);

    ApiResponseDto getAllPlans();

    void deletePlan(Integer planId);
}
