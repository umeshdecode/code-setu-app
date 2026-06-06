package com.cargosetu.shifting.repository;

import com.cargosetu.shifting.entity.ShiftInventoryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftInventoryPhotoRepository extends JpaRepository<ShiftInventoryPhoto, Long> {
}
