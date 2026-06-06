package com.cargosetu.shifting.repository;

import com.cargosetu.customer.entity.PackagingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackagingMaterialRepository extends JpaRepository<PackagingMaterial, Long> {
}
