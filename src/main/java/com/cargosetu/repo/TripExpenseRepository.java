package com.cargosetu.repo;

import com.cargosetu.model.TripExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripExpenseRepository extends JpaRepository<TripExpense, Long> {
    List<TripExpense> findByTripTripId(Long tripId);
}
