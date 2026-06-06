package com.cargosetu.customer.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tblt_shift_item")
public class ShiftItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    private String itemName;

    private String category;

    private String packageNumber;

    private String conditionStatus;

    private String remarks;

    private Boolean packed = false;

    private Boolean loaded = false;

    private Boolean unloaded = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ShiftRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shifting_id")
    private Shifting shifting;
}