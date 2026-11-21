package com.cargosetu.repo;

import com.cargosetu.cache.dto.ClientServiceNameValueProjection;
import com.cargosetu.model.Clientservice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientServiceRepo  extends JpaRepository<Clientservice,Integer> {
    boolean existsByNameAndValue(String name,String value);

    Optional<Clientservice> findByName(String name);
    @Query("""
        SELECT c.name AS name, c.value AS value
        FROM Clientservice c
    """)
    List<ClientServiceNameValueProjection> findAllNameAndValue();
}
