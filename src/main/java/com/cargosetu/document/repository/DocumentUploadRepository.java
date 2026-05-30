package com.cargosetu.document.repository;

import com.cargosetu.document.model.DocumentUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentUploadRepository extends JpaRepository<DocumentUpload, Long> {
}
