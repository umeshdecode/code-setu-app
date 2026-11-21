package com.cargosetu.customer.entity;

import com.cargosetu.shifting.entity.ShiftInventoryItem;
import jakarta.persistence.*;


@Entity
@Table(name = "tblt_shift_item_damage")
public class ShiftItemDamage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long damageId;

    private String damageType;

    private String description;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private ShiftInventoryItem shiftItem;
}