package com.cargosetu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

@Entity
@Data
public class Template {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String type; // QUOTATION ,INVOICE etc

    @Lob
    private String htmlContent;

    private Boolean isActive;

    private String filePath;

    private Long companyId;

    private boolean isFavoriteTemplate = false;
}
