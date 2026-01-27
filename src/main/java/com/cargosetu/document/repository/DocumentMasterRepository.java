package com.cargosetu.document.repository;

import com.cargosetu.document.model.DocumentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentMasterRepository extends JpaRepository<DocumentMaster, Long> {

}
