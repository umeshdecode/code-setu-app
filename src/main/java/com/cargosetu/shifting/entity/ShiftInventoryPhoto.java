package com.cargosetu.shifting.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "shift_inventory_photo")
@Data
public class ShiftInventoryPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private ShiftInventoryItem inventoryItem; // Link to the inventory item

    @Column(nullable = false)
    private String photoUrl;

    private String photoType; // e.g., "before_packing", "after_packing", "damage"
}
