package com.cargosetu.customer.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "tblm_packaging_material")
public class PackagingMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialId;

    private String materialName;

    private String unit;
}