package com.cargosetu.repo;
import com.cargosetu.model.Plans;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plans, Integer> {
    @Query(value = "SELECT * FROM tblmplans WHERE plan_id = :planId", nativeQuery = true)
    Optional<Plans> findByPlanId(@Param("planId") Integer planId);
}

