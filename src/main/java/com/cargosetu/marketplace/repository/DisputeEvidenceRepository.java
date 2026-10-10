package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.DisputeEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidence, Long> {
    List<DisputeEvidence> findByDisputeId(Long disputeId);
}
