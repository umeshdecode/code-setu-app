package com.cargosetu.customer.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tblm_packaging_material")
@Data
public class PackagingMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialId;

    private String materialName;

    private String unit;
    @Column(nullable = false)
    private Boolean active = true;
}