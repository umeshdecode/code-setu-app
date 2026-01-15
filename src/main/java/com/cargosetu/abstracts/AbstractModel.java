package com.cargosetu.abstracts;

import com.cargosetu.utils.ApplicationUtils;
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

    @PrePersist
    private void setCreateDate(){
        createdAt = LocalDateTime.now();
        createdByName = ApplicationUtils.getCurrentUser().getFirstName() +  " " + ApplicationUtils.getCurrentUser().getLastName();
        createdBy = ApplicationUtils.getCurrentUser().getUserId().longValue();
    }
    @Column(name = "updated_at", updatable = false)
    private LocalDateTime updatedAt;


}
