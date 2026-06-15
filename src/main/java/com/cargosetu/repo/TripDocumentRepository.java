package com.cargosetu.repo;

import com.cargosetu.model.TripDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripDocumentRepository extends JpaRepository<TripDocument, Long> {
    List<TripDocument> findByTripTripId(Long tripId);
}
