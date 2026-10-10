package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.TripEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripEventRepository extends JpaRepository<TripEvent, Long> {
    List<TripEvent> findByTripIdOrderByCreatedAtAsc(Long tripId);
    List<TripEvent> findByTripIdAndEventType(Long tripId, String eventType);
}
