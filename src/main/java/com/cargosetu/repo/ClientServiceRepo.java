package com.cargosetu.repo;

import com.cargosetu.model.Clientservice;
import com.cargosetu.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientServiceRepo  extends JpaRepository<Clientservice,Integer> {
    boolean existsByNameAndValue(String name,String value);

    Optional<Clientservice> findByName(String name);
}
