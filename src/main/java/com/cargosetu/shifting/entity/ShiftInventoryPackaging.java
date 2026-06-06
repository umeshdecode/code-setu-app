package com.cargosetu.shifting.entity;

import com.cargosetu.customer.entity.PackagingMaterial;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "shift_inventory_packaging")
@Data
public class ShiftInventoryPackaging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private ShiftInventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_material_id", nullable = false)
    private PackagingMaterial packagingMaterial;

    @Column(nullable = false)
    private Double quantityUsed;
}
