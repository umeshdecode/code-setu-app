package com.cargosetu.service;

import com.cargosetu.dto.PlanDTO;

import java.util.List;

public interface PlanService {

    PlanDTO createPlan(PlanDTO planDTO);

    PlanDTO updatePlan(Integer planId, PlanDTO planDTO);

    PlanDTO getPlanById(Integer planId);

    List<PlanDTO> getAllPlans();

    void deletePlan(Integer planId);
}
