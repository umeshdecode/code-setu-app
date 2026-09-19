package com.cargosetu.loadmatch.entity;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tblm_shipment_item")
@Data
public class ShipmentItem extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shipmentItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    private String itemName;

    private Double weightKg;

    private Double volumeCbm;

    private Double lengthCm;

    private Double widthCm;

    private Double heightCm;

    private Integer quantity;

    private Boolean fragile;

    private Boolean stackable;
}
