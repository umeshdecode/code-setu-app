package com.cargosetu.service.impl;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.dto.PlanDTO;
import com.cargosetu.model.Plans;
import com.cargosetu.repo.PlanRepository;
import com.cargosetu.service.PlanService;
import com.cargosetu.utils.ApplicationUtils;
import com.cargosetu.utils.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Autowired
    private ApplicationUtils applicationUtils;

    @Override
    public ApiResponseDto createPlan(PlanDTO dto) {

        log.info("Request received to create plan : {}", dto.getName());

        Plans plan = mapToEntity(dto);

        plan.setCreatedBy(applicationUtils.getLoggedInStaffId());
        plan.setCreatedByName(
                applicationUtils.getLoggedInUser().getFirstName());

        plan.setCreatedAt(LocalDateTime.now());

        Plans savedPlan = planRepository.save(plan);

        log.info("Plan created successfully. PlanId={}",
                savedPlan.getPlanId());

        return ApiResponseDto.<PlanDTO>builder()
                .responseCode(HttpStatus.SC_OK)
                .responseMessage(CommonConstants.PlanConstant.PLAN_CREATED)
                .responseBody(mapToDTO(savedPlan))
                .build();
    }

    @Override
    public ApiResponseDto updatePlan(Integer planId,
                                              PlanDTO dto) {

        log.info("Updating plan. PlanId={}", planId);

        Plans plan = planRepository.findById(planId)
                .orElseThrow(() -> {

                    log.warn("Plan not found. PlanId={}",
                            planId);

                    return new ResourceNotFoundException(
                            "Plan",
                            "planId",
                            new Exception());
                });

        plan.setPlanName(dto.getName());
        plan.setPlanValidity(dto.getPlanValidity());
        plan.setPrice(dto.getPrice());
        plan.setPlanCategory(dto.getPlannCategory());

        plan.setUpdatedById(applicationUtils.getLoggedInStaffId());

        plan.setUpdatedByName(
                applicationUtils.getLoggedInUser().getFirstName());

        plan.setUpdatedAt(LocalDateTime.now());

        Plans updated = planRepository.save(plan);

        log.info("Plan updated successfully. PlanId={}",
                updated.getPlanId());

        return ApiResponseDto.builder().responseCode(HttpStatus.SC_OK)
                .responseMessage(CommonConstants.PlanConstant.PLAN_UPDATED)
                .responseBody(mapToDTO(updated))
                .build();
    }

    @Override
    public ApiResponseDto getPlanById(Integer planId) {

        log.debug("Fetching plan. PlanId={}", planId);

        Plans plan = planRepository.findById(planId)
                .orElseThrow(() -> {

                    log.warn("Plan not found. PlanId={}",
                            planId);

                    return new ResourceNotFoundException(
                            "Plan",
                            "planId",
                            new Exception());
                });

        return ApiResponseDto.<PlanDTO>builder()
                .responseCode(HttpStatus.SC_OK)
                .responseMessage(CommonConstants.PlanConstant.PLAN_FETCHED)
                .responseBody(mapToDTO(plan))
                .build();
    }

    @Override
    public ApiResponseDto getAllPlans() {

        log.debug("Fetching all plans");

        List<PlanDTO> plans = planRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();

        log.info("Total plans fetched : {}", plans.size());

        return ApiResponseDto.<List<PlanDTO>>builder()
                .responseCode(HttpStatus.SC_OK)
                .responseMessage(CommonConstants.PlanConstant.PLAN_LIST_FETCHED)
                .responseBody(plans)
                .build();
    }

    @Override
    public void deletePlan(Integer planId) {

        Plans plan = planRepository.findById(planId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plan",
                                "planId",
                                new Exception()));

        plan.setIsDeleted(true);
        plan.setIsActive(false);

        plan.setUpdatedById(applicationUtils.getLoggedInStaffId());

        plan.setUpdatedByName(
                applicationUtils.getLoggedInUser().getFirstName());

        plan.setUpdatedAt(LocalDateTime.now());

        planRepository.save(plan);

        log.info("Plan soft deleted. PlanId={}",
                planId);
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
