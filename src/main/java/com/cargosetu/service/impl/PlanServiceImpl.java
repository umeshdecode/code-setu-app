package com.cargosetu.service.impl;

import com.cargosetu.dto.PlanDTO;
import com.cargosetu.model.Plans;
import com.cargosetu.repo.PlanRepository;
import com.cargosetu.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Override
    public PlanDTO createPlan(PlanDTO dto) {
        log.info("Creating new plan with name: {}", dto.getName());

        Plans plan = mapToEntity(dto);
        Plans saved = planRepository.save(plan);

        log.info("Plan created successfully with ID: {}", saved.getPlanId());
        return mapToDTO(saved);
    }

    @Override
    public PlanDTO updatePlan(Integer planId, PlanDTO dto) {
        log.info("Updating plan with ID: {}", planId);

        Plans plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    log.error("Plan not found for update, ID: {}", planId);
                    return new RuntimeException("Plan not found");
                });

        plan.setPlanName(dto.getName());
        plan.setPlanValidity(dto.getPlanValidity());
        plan.setPrice(dto.getPrice());
        plan.setPlanCategory(dto.getPlannCategory());

        Plans updated = planRepository.save(plan);
        log.info("Plan updated successfully, ID: {}", planId);

        return mapToDTO(updated);
    }

    @Override
    public PlanDTO getPlanById(Integer planId) {
        log.debug("Fetching plan by ID: {}", planId);

        return planRepository.findById(planId)
                .map(this::mapToDTO)
                .orElseThrow(() -> {
                    log.warn("Plan not found, ID: {}", planId);
                    return new RuntimeException("Plan not found");
                });
    }

    @Override
    public List<PlanDTO> getAllPlans() {
        log.debug("Fetching all plans");

        List<PlanDTO> plans = planRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        log.info("Total plans fetched: {}", plans.size());
        return plans;
    }

    @Override
    public void deletePlan(Integer planId) {
        log.info("Deleting plan with ID: {}", planId);

        Plans plan = planRepository.findById(planId)
                .orElseThrow(() -> {
                    log.error("Plan not found for delete, ID: {}", planId);
                    return new RuntimeException("Plan not found");
                });

        planRepository.delete(plan);
        log.info("Plan deleted successfully, ID: {}", planId);
    }

    /* ---------- Mapper Methods ---------- */

    private Plans mapToEntity(PlanDTO dto) {
        Plans plan = new Plans();
        plan.setPlanName(dto.getName());
        plan.setPlanValidity(dto.getPlanValidity());
        plan.setPrice(dto.getPrice());
        plan.setPlanCategory(dto.getPlannCategory());
        return plan;
    }

    private PlanDTO mapToDTO(Plans plan) {
        PlanDTO dto = new PlanDTO();
        dto.setPlanId(plan.getPlanId());
        dto.setName(plan.getPlanName());
        dto.setPlanValidity(plan.getPlanValidity());
        dto.setPrice(plan.getPrice());
        dto.setPlannCategory(plan.getPlanCategory());
        return dto;
    }
}
