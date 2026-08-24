package com.cargosetu.repo;

import com.cargosetu.model.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    Optional<Template> findByType(String type);

    Page<Template> findByTypeOrderByIsFavoriteTemplateDesc(String type, Pageable pageable);

    Optional<Template> findByTypeAndIsFavoriteTemplateTrue(String type);
}
