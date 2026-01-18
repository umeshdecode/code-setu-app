package com.cargosetu.abstracts;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
public abstract class AbstractModel {

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by_name")
    private String updatedByName;

    @Column(name = "updated_by_id")
    private Long updatedById;
    @PrePersist
    private void setCreateDate(){
        createdAt = LocalDateTime.now();
    }
    @Column(name = "updated_at", updatable = false)
    private LocalDateTime updatedAt;


}
