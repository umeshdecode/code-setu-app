package com.cargosetu.shifting.entity;

import com.cargosetu.customer.entity.Shifting;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shift_inventory_item")
@Data
public class ShiftInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shifting shifting;

    @Column(nullable = false)
    private String roomName;

    @Column(nullable = false)
    private String itemName;

    private String category;

    @Column(name = "package_number")
    private String packageNumber;

    private String remarks;

    @Column(nullable = false)
    private String conditionStatus; // e.g., "Good", "Minor Dent", "Scratch"

    @Column(nullable = false)
    private Boolean packed = false;

    @Column(nullable = false)
    private Boolean loaded = false;

    @Column(nullable = false)
    private Boolean unloaded = false;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShiftInventoryPhoto> shiftInventoryPhotos = new ArrayList<>();

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShiftInventoryPackaging> shiftInventoryPackagings = new ArrayList<>();
}
