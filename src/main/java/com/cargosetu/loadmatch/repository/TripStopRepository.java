package com.cargosetu.loadmatch.repository;

import com.cargosetu.loadmatch.entity.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripStopRepository extends JpaRepository<TripStop, Long> {
    List<TripStop> findByTrip_TripIdOrderBySequenceOrderAsc(Long tripId);
}
