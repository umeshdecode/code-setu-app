package com.cargosetu.repository;
import com.cargosetu.model.Plans;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plans, Integer> {
}

