package com.cargosetu.shifting.repository;

import com.cargosetu.shifting.entity.ShiftInventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftInventoryItemRepository extends JpaRepository<ShiftInventoryItem, Long> {
    List<ShiftInventoryItem> findByShifting_ShiftingIdOrderByIdAsc(Long shiftId);
}
