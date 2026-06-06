package com.cargosetu.shifting.repository;

import com.cargosetu.shifting.entity.ShiftInventoryPackaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftInventoryPackagingRepository extends JpaRepository<ShiftInventoryPackaging, Long> {
}
