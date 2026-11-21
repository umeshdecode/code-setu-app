package com.cargosetu.repository;

import com.cargosetu.entity.TruckOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TruckOptionRepository extends JpaRepository<TruckOption, Long> {

    List<TruckOption> findByActiveTrueAndIsDeletedFalse();

    List<TruckOption> findByIsDeletedFalse();

    Optional<TruckOption> findByTruckTypeIgnoreCaseAndIsDeletedFalse(String truckType);

    boolean existsByTruckTypeIgnoreCaseAndIsDeletedFalse(String truckType);
}
