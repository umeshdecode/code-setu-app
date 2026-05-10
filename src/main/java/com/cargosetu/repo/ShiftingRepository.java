package com.cargosetu.repo;

import com.cargosetu.customer.entity.Shifting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftingRepository extends JpaRepository<Shifting, Long> {
}
